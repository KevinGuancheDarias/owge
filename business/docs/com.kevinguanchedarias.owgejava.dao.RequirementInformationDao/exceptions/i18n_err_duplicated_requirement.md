I18N_ERR_DUPLICATED_REQUIREMENT
=================================

# Summary 

This error is thrown when trying to insert a new requirement information that already exists for the target referenceId. The entity key is built  by relationId + requirementType + secondValue

# Noncompliant example

1. POST /entity/1/requirements
```json
{
	"requirement": {
		"code": "BEEN_RACE"
	},
	"secondValue": 4
}
```
2. Other POST to /entity/1/requirements (resulting in this error)
```json
{
	"requirement": {
		"code": "BEEN_RACE"
	},
	"secondValue": 4
}
```

# Compliant examples

## Where target referenceId is different 
1. POST /entity/**1**/requirements

```json
{
	"requirement": {
		"code": "BEEN_RACE"
	},
	"secondValue": 8
}
```

2. POST /entity/**2**/requirements

```json
{
	"requirement": {
		"code": "BEEN_RACE"
	},
	"secondValue": 8
}
```

## Where target requirement is different
1. POST /entity/1/requirements

```json
{
	"requirement": {
		"code": "HAVE_UNIT"
	},
	"secondValue": 2
}
```

2. POST /entity/**2**/requirements

```json
{
	"requirement": {
		"code": "BEEN_RACE"
	},
	"secondValue": 8
}
```

## Where target secondValue is different

1. POST /entity/1/requirements

```json
{
	"requirement": {
		"code": "BEEN_RACE"
	},
	"secondValue": 8
}
```

2. POST /entity/**2**/requirements

```json
{
	"requirement": {
		"code": "BEEN_RACE"
	},
	"secondValue": 6
}
```