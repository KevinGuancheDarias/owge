I18N_ERR_INVALID_TYPE
=======================

# Summary

This error occurs when the sent value for `type` is not compatible with [`ImprovementTypeEnum`](../../../src/main/java/com/kevinguanchedarias/owgejava/enumerations/ImprovementTypeEnum.java), you must use a value available in the ENUM

# Noncompliant example

```json
{
	"type": "doesn't exists",
	"unitTypeId": 4,
	"value": 10
} 
```

# Compliant example
```json
{
	"type": "SHIELD",
	"unitTypeId": 4,
	"value": 10
} 
```