I18N_ERR_INVALID_UNIT_TYPE
=============================

# Summary

This error occurs when the sent value for `unitTypeId` is non-numeric, or it's passed as `0`

# Noncompliant example
```json
{
	"type": "ATTACK",
	"unitTypeId": null,
	"value": 10
} 
```

# Compliant example
There is a unit type in the database with the id `7`
```json
{
	"type": "ATTACK",
	"unitTypeId": 7,
	"value": 10
} 
```

