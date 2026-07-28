# Save fixtures

Committed golden saves for compatibility smoke tests.

| File | Format | Notes |
|:---|:---|:---|
| `v1.4e-fresh-league.cfb.gz` | gzip of `SaveManager` `L:` format (`League.CURRENT_SAVE_VERSION`) | Fresh dynasty with a user coach; used by `GoldenSaveFixtureTest` |

## Regenerate

From the repo root:

```bash
./gradlew -p desktop-standalone :engine:test \
  --tests simulation.GoldenSaveFixtureTest \
  -DregenGoldenSaves=true
```

Then commit the updated `.cfb.gz` if format or default universe data changed.
