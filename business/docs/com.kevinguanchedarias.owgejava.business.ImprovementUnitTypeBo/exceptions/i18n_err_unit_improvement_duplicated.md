I18N_ERR_UNIT_IMPROVEMENT_DUPLICATED
=======================================

# Summary

This error is thrown when there is a duplicated unit improvement... obvious I know, let me explain it better. When you are creating a unit type of the same `type` targetting the same `unitType` it's considered a duplicated.

# Noncompliant example
```json
{
    "type": "ATTACK",
    "unitTypeId": 4,
    "value": 20
}
...
{
    "type": "ATTACK",
    "unitTypeId": 4,
    "value": 10
}
```

# Compliant
```json
{
    "type": "ATTACK",
    "unitTypeId": 4,
    "value": 20
}
...
{
    "type": "ATTACK",
    "unitTypeId": 5,
    "value": 10
}
```