I18N_ERR_IMPROVEMENT_HAS_NO_SUCH_UNIT_IMPROVEMENT
===================================================

# Summary

This error is thrown when you tried to do something with a unit type improvement, owned by a specified improvement, this error is returned even if there is a unit type improvement with the given id, because the rule, that applyes here, is that, it **MUST** belong to the specified improvement

# Hint

You may use the `affectedItem.id` property to create an easy to display translatable message in the frontend