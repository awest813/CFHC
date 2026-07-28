package desktop;

import simulation.League;
import simulation.PlatformLog;
import simulation.SeasonController;
import simulation.SeasonFlowOrder;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bulk season advance for the desktop shell.
 *
 * <p>Per {@code docs/THREADING.md}, league mutation runs on the EDT (the desktop
 * game/UI thread). The background worker only drives progress UI and waits on
 * each {@link SeasonController#advanceWeek()} via {@link SwingUtilities#invokeAndWait}.
 */
final class DesktopBulkSimulator {
    private static final String TAG = "DesktopBulkSimulator";

    interface Host {
        JFrame window();
        League league();
        DesktopUiBridge bridge();
        SeasonController controller();
        void resolvePendingUserDiscipline();
        void markDirty();
        void afterBulkRefresh();
        void onRecruitingGateFromBulk();
        void onNewSeasonFromBulk();
        String seasonPeriodLabel();
        int maxFullYearSteps();
    }

    private final Host host;

    DesktopBulkSimulator(Host host) {
        this.host = host;
    }

    void simulateToTargetWeek(int targetWeek) {
        League league = host.league();
        if (targetWeek <= league.currentWeek) {
            try {
                JOptionPane.showMessageDialog(host.window(),
                        DesktopTheme.messageForDialog(
                                "This league is already at or beyond that point in the season.\n"
                                        + "Use Play Next Week or Advance Through Offseason instead."),
                        "Nothing to Simulate",
                        JOptionPane.INFORMATION_MESSAGE);
            } finally {
                // Always clear bulkRunning even if the dialog cannot display (headless).
                host.afterBulkRefresh();
            }
            return;
        }

        SimulationProgressDialog dialog = new SimulationProgressDialog(host.window(), "Season Simulation");
        int startWeek = league.currentWeek;
        int maxWeeks = Math.max(1, targetWeek - startWeek);
        DesktopUiBridge bridge = host.bridge();

        SwingWorker<Integer, String> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() throws Exception {
                bridge.setSuppressBlockingUi(true);
                try {
                    int played = 0;
                    while (league.currentWeek < targetWeek
                            && !bridge.isNewSeasonPending()
                            && !bridge.isAwaitingDockedRecruiting()) {
                        if (dialog.isCancelled()) {
                            break;
                        }
                        advanceOneWeekOnEdt();
                        played++;
                        int progress = (int) ((float) played / maxWeeks * 100);
                        setProgress(Math.min(100, progress));
                        publish("Playing Week " + league.currentWeek);
                    }
                    return played;
                } finally {
                    bridge.setSuppressBlockingUi(false);
                }
            }

            @Override
            protected void process(List<String> chunks) {
                dialog.setStatus(chunks.get(chunks.size() - 1));
                dialog.setProgress(getProgress());
            }

            @Override
            protected void done() {
                dialog.dispose();
                try {
                    int played = get();
                    if (played > 0) {
                        host.markDirty();
                    }
                    PlatformLog.i(TAG, "Simulated " + played + " weeks.");
                    if (dialog.isCancelled()) {
                        JOptionPane.showMessageDialog(host.window(),
                                DesktopTheme.messageForDialog(
                                        "Season simulation was interrupted after " + played + " week(s)."),
                                "Simulation Interrupted",
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    PlatformLog.w(TAG, "Season simulation interrupted.");
                } catch (java.util.concurrent.ExecutionException e) {
                    PlatformLog.e(TAG, "Season simulation failed.",
                            e.getCause() != null ? e.getCause() : e);
                    JOptionPane.showMessageDialog(host.window(),
                            DesktopTheme.messageForDialog("Season simulation encountered an error:\n"
                                    + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage())),
                            "Simulation Error", JOptionPane.ERROR_MESSAGE);
                }
                finishBulkUi();
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                dialog.setProgress((Integer) evt.getNewValue());
            }
        });
        worker.execute();
        dialog.setVisible(true);
    }

    void simulateThroughPostseason() {
        simulateToTargetWeek(SeasonFlowOrder.firstOffseasonWeek(host.league().regSeasonWeeks));
    }

    void advanceFullYear() {
        SimulationProgressDialog dialog = new SimulationProgressDialog(host.window(), "Full-Year Simulation");
        dialog.setIndeterminate(true);
        dialog.setStatus("Advancing " + host.seasonPeriodLabel() + "...");
        DesktopUiBridge bridge = host.bridge();
        bridge.clearNewSeasonPending();
        long start = System.currentTimeMillis();

        SwingWorker<Integer, String> worker = new SwingWorker<>() {
            private boolean limitReached = false;
            private boolean cancelled = false;

            @Override
            protected Integer doInBackground() throws Exception {
                bridge.setSuppressBlockingUi(true);
                try {
                    int played = 0;
                    while (!bridge.isNewSeasonPending()) {
                        if (dialog.isCancelled()) {
                            cancelled = true;
                            break;
                        }
                        if (bridge.isAwaitingDockedRecruiting()) {
                            break;
                        }
                        advanceOneWeekOnEdt();
                        played++;
                        publish("Advancing " + host.seasonPeriodLabel()
                                + " (Week " + host.league().currentWeek + ")");
                        if (played >= host.maxFullYearSteps()) {
                            limitReached = true;
                            break;
                        }
                    }
                    return played;
                } finally {
                    bridge.setSuppressBlockingUi(false);
                }
            }

            @Override
            protected void process(List<String> chunks) {
                dialog.setStatus(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                dialog.dispose();
                int played = 0;
                try {
                    played = get();
                } catch (InterruptedException ie) {
                    PlatformLog.w(TAG, "Full-year simulation interrupted while collecting result.", ie);
                    Thread.currentThread().interrupt();
                    host.afterBulkRefresh();
                    return;
                } catch (java.util.concurrent.ExecutionException ee) {
                    Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
                    PlatformLog.e(TAG, "Full-year simulation failed.", cause);
                    JOptionPane.showMessageDialog(host.window(),
                            DesktopTheme.messageForDialog("Full-year simulation encountered an error:\n"
                                    + cause.getMessage()),
                            "Simulation Error", JOptionPane.ERROR_MESSAGE);
                    host.afterBulkRefresh();
                    return;
                }

                if (played > 0) {
                    host.markDirty();
                }
                PlatformLog.i(TAG, "Advanced full year (" + played + " steps) in "
                        + (System.currentTimeMillis() - start) + "ms");

                if (limitReached) {
                    JOptionPane.showMessageDialog(host.window(),
                            DesktopTheme.messageForDialog(
                                    "Simulation stopped after " + host.maxFullYearSteps()
                                            + " steps without completing the season.\n"
                                            + "This may indicate a simulation bug. Save your league and report the issue."),
                            "Simulation Limit Reached", JOptionPane.WARNING_MESSAGE);
                } else if (cancelled) {
                    JOptionPane.showMessageDialog(host.window(),
                            DesktopTheme.messageForDialog("Full-year simulation was interrupted."),
                            "Simulation Interrupted",
                            JOptionPane.INFORMATION_MESSAGE);
                }

                if (bridge.isNewSeasonPending()) {
                    host.onNewSeasonFromBulk();
                    return;
                }
                host.afterBulkRefresh();
                if (bridge.isAwaitingDockedRecruiting()) {
                    host.onRecruitingGateFromBulk();
                }
            }
        };

        worker.execute();
        dialog.setVisible(true);
    }

    private void finishBulkUi() {
        DesktopUiBridge bridge = host.bridge();
        if (bridge.isNewSeasonPending()) {
            host.onNewSeasonFromBulk();
            return;
        }
        host.afterBulkRefresh();
        if (bridge.isAwaitingDockedRecruiting()) {
            host.onRecruitingGateFromBulk();
        }
    }

    private void advanceOneWeekOnEdt() throws InterruptedException, InvocationTargetException {
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        Runnable step = () -> {
            try {
                host.controller().advanceWeek();
                host.resolvePendingUserDiscipline();
            } catch (RuntimeException ex) {
                error.set(ex);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            step.run();
        } else {
            SwingUtilities.invokeAndWait(step);
        }
        if (error.get() != null) {
            throw error.get();
        }
    }
}
