I18N_ERR_GENERIC_ITEM_NOT_FOUND
================================

# Summary
This error is thrown when the system tried to fetch an entity with the specified id that doesn't exists

# Exception information in the REST response
As you can see below, `affectedItem` contains the not existing id and the `type` of the entity, which may be used in the frontend for language translation

```json
{
    "message": "I18N_ERR_GENERIC_ITEM_NOT_FOUND",
    "exceptionType": "NotFoundException",
    "extra": {
        "affectedItem": {
            "id": 2,
            "type": "com.kevinguanchedarias.owgejava.entity.UnitType"
        }
    },
    "reporterAsString": "com.kevinguanchedarias.owgejava.exception.NotFoundException"
}
```


