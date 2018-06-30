# SGT changelog

v0.3.1
===========
* __Improvement:__ Allow all subdomains from kevinguanchedarias.com to login into account system
* __Fix:__ Admin panel doesn't allow to define attack, health, and shield values, it should allow, and the value should be mandatory or by default 0, 1 for health, database, should not even allow null for "health" parameter
* __Fix:__ When adding an image to an unsaved Upgrade, Unit, Faction or special location all the information is removed
* __Known issue:__ NTuL3pTw: When uploading all faction images, can't do it all at once, bogus upload functionality in JSF framework =/

v0.3.0
===========
* __Merge:__ v0.2.1
* __Fix:__ Endpoint that returns the units in my planet, is also returning enemy units
* __Fix:__ After migrating to Angular 5, upgrades component suddenly stopped working
* __Feature:__ As game admin, I would like to be able to use different frontend versions
* __Feature:__ As a user, I would like to logout
* __Feature:__ As a user I would like to see missions been realized to my planets, by other users
* __Feature:__ As a user I would like to see my running missions
* __Feature:__ As a player I would like to conquest an enemy planet
* __Feature:__ As a player, I would like to counterattack in one of my own planets
* __Feature:__ As a player, I would like to attack other player
* __Feature:__ As a player I would like to be able to explore a planet with my units
* __Feature:__ As a player, I would like to gather resources from a planet
* __Feature:__ As a admin, I would like to establish the max number of planets for given faction
* __Feature:__ As a player, I would like to establish a base in a new planet

v0.2.1
===========
* __Fix:__ Fix Websocket not authenticating automatically
* __Fix:__ Global loading functionality not working as expected
* __Fix:__ Fix a strange error that occurs when the sendMessage() is invoked too quickly after another sendMessage
* __Fix:__ Login webpage doesn't redirect to Game index if already logged in
* __Fix:__ When a user clicks navigate, the Galaxy dissapears from the selection, while it still works as selected
* __Fix:__ When the navigation page is loaded the first, won't work
* __Improvement:__ Show the current navigation somewhere

v0.2.0
===========
* __Feature:__ As a player I would like to navigate
* __Feature:__ As a player I don't want to see planets that are not explored

v0.1.0
===========
* __Feature:__ A as player I would like to login
* __Feature:__ As As a player i would like to select a public universe and subscribe if required
* __Feature:__ As a player I would like to see unlocked upgrades
* __Feature:__ As a player I would like to be able to do upgrades
* __Feature:__ A a player I would like to see unlocked units
* __Feature:__ As a player I would like to unlock units when upgrade reach the required level
* __Feature:__ As a player I would like to build a unit
* __Feature:__ As a player I would like to cancel the build unit mission
* __Feature:__ A as player I would like to see the units I have in my selected planet