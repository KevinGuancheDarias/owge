# SGT changelog

v0.7.3 (latest)
==================
* __Fix:__ In some resolutions the player can't see the entire sidebar
* __Fix:__ Attacks are not working when one user with an alliance attacks a user that doesn't have an alliance
* __Fix:__ When the user sends a mission, the available units count is not updating unless full window refresh is issued `(see video evidence` [here](assets/changelog/bugs/pwY76NOb.mp4)`)`

v0.7.2 (2019-02-25 14:54)
===========================
* __Improvement:__ Display a cancel confirmation in upgrades  
![Display a cancel confirmation in upgrades](assets/changelog/improvements/kBMYJtvv.png)  
* __Fix:__ Close icon not displaying in modals
* __Fix:__ Sometimes units don't appear, but, it's counting the number of soldiers
* __Fix:__ Register and forgot password links are inverted

v0.7.1 (2019-02-24 23:01)
==================
* __Fix:__ "Register" and "forgot password" buttons not pointing to KGDW links
* __Fix:__ Fix missing icons for mobile menu
* __Fix:__ Fix missing Ranking title
* __Fix:__ Fix spanish accents not displaying properly  
![Fix spanish accents not displaying properly](assets/changelog/bugs/NTd6k5uD.png)  

v0.7.0 (2019-02-21 15:42)
===========================
* __Feature:__ As a player I want to have a ranking with at least battle points  
![As a player I want to have a ranking with at least battle points](assets/changelog/features/L46uhduW.png)  
* __Feature:__ As a player, I want to be able to create an alliance  
![As a player, I want to be able to create an alliance](assets/changelog/features/O4xthVje.png)  
* __Feature:__ Change login system to use KGDW (:warning: Breaks old accounts :warning:)
* __Feature:__ As a player I Want to be able to deploy my units to other planets  
![As a player I Want to be able to deploy my units to other planets](assets/changelog/features/4a028iCy.png)  
* __Fix:__ If closing the modal by clicking outside is disabled, the background fade will not allow the user to click anything

v0.6.3 (2019-02-23 18:05)
===========================
* __Improvement:__ Apply Kc style  
![Apply Kc style](assets/changelog/improvements/CeUi35X0.png)  

v0.6.2 (2018-12-27 14:44)
===========================
* __Fix:__ Missing fields in database creation script

v0.6.1 (2018-09-27 16:03)
===========================
* __Merge:__ v0.5.7
* __Merge:__ v0.5.6

v0.6.0 (2018-09-19 09:27)
==========================
* __Merge:__ v0.5.5
* __Feature:__ As an administrator I want to define what missions each type of unit can do  
![As an administrator I want to define what missions each type of unit can do](assets/changelog/features/QNGBLo0m.png)  
* __Feature:__ As a player i don't want to be able to use selected types of units for missions, (for example Defenses, should only be allowed to use Deploy missions to owned planets)

v0.5.7 (2018-09-27 15:28)
=================

* __Fix:__ Bad format for units inherintance system in Angular
* __Merge:__ v0.4.4

v0.5.6 (2018-09-11 14:44)
==================
* __Fix:__ When attacking an user that has heroes that provide more research speed, the attack hangs  

v0.5.5 (2018-09-10 14:35)
===============

* __Fix:__ Attacks crash sometimes under race circumstances

v0.5.4 (2018-09-08 13:30)
===============

* __Fix:__ Fix again, because the fix ["The user can't build when the user has not consumed any energy, regression of 5jLeBzrv"](https://trello.com/c/AOhmFxTI/50-the-user-cant-build-when-the-user-has-not-consumed-any-energy-regression-of-5jlebzrv) was not working

v0.5.3 (2018-09-08 02:18)
===============

* __Fix:__ The user can't build when the user has not consumed any energy, regression of [5jLeBzrv](https://trello.com/c/5jLeBzrv/48-backend-allows-the-user-to-build-over-his-energy-backend-is-not-checking-that-the-required-energy-is-met)
* __Fix:__ Changelog is opening the external links in the same page
* __Improvement:__ Display a filter by type in upgrades  
![Display a filter by type in upgrades](assets/changelog/improvements/DZWLdNLi.png)  

v0.5.2 (2018-09-01 17:58)
=========
* __Merge:__ v0.4.2
* __Security:__ Backend allows the user to build over his energy, backend is not checking that the required energy is met
* __Fix:__ Deploys are failing, when multiple deploys to the same target planet are using the same kind of unit  
![Deploys are failing, when multiple deploys to the same target planet are using the same kind of unit](assets/changelog/bugs/UkfqMsJB.png)  
* __Improvement:__ Display a filter by type in units  
![Display a filter by type in units](assets/changelog/improvements/SC8fU3lW.png)

v0.5.1 (2018-08-21 13:55)
=========
* __Fix:__ u1 Import script not importing More Units improvement

v0.5.0 (2018-08-19 18:41)
==========
* __Merge:__ v0.4.1
* __Fix:__ Backend not sending correct number of "count" of the running unit build mission
* __Feature:__ As a player I want to have heroes
* __Feature:__ As an admin I want to be able to add an improvement to increase unit build speed  
![As an admin I want to be able to add an improvement to increase unit build speed](assets/changelog/features/kemyo8Ao.png)  
* __Feature:__ As an admin I wan to be able to add an improvement to upgrade research speed  
![As an admin I wan to be able to add an improvement to upgrade research speed](assets/changelog/features/fTtxVUv4.png)  
* __Feature:__ As an admin I want to be able to define an unit as unique  
![As an admin I want to be able to define an unit as unique](assets/changelog/features/McD3CIuL.gif)  
* __Feature:__ As a player I want to have a limit on determinated types of units, for example in troops
* __Feature:__ As an admin i want to add  unit type amount limit
* __Feature:__ As a player I want energy to work like expected
* __Feature:__ As a player I want to be able to deploy units in other of my planets
* __Feature:__ As a player, I want to see the unit base attributes
* __Feature:__ As a player I want to see the gather report

v0.4.4 (2018-09-27 14:38)
==========================
* __Merge:__ v0.3.8

v0.4.3 (2018-09-10 14:58)
=======
* __Merge:__ v0.3.7

v0.4.2 (2018-08-19 16:11)
====================
* __Merge:__ v0.3.6

v0.4.1 (2018-08-09 19:15)
=======
* __Merge:__ v0.3.5

v0.4.0 (2018-07-29 22:20)
==========
* __Feature:__ As a player I want to be able to voluntary leave a planet
* __Fix:__ Universe without custom frontend URL is not working in 0.4.x
* __Feature:__ As a player I would like to cancel my running unit missions
* __Feature:__ As a pleyer, I would like to be able to delete my own units
* __Feature:__ As a player I would like to see the requirements for the units of my race
* __Merge:__ v0.3.4

v0.3.8 (2018-09-27 11:55)
============
* __Improvement:__ Display time in days, hours, minutes, and seconds  
![Display time in days, hours, minutes, and seconds](assets/changelog/improvements/HYldODUu.png)  
* __Improvement:__ Increase game load speed, decrease data usage and battery consumption  
* __Improvement:__ Add little mechanism to ensure failing missions do not crash the game  
![Add little mechanism to ensure failing missions do not crash the game](assets/changelog/improvements/bXXGVwDq.gif) 
* __Improvement:__ Reduce server RAM usage

v0.3.7 (2018-09-10 14:52)
===========
* __Fix:__ Attacks crash sometimes under race circumstances

v0.3.6 (2018-08-19 16:07)
==========
* __Fix:__ Attacks do not remove units, unless all the unit count is death  
![Attacks do not remove units, unless all the unit count is death](assets/changelog/bugs/ID3x4EeA.gif)  
* __Fix:__ Send mission dialog can't be closed in mobile devices  
![Send mission dialog can't be closed in mobile devices](assets/changelog/bugs/2wBzKBba.png)  

v0.3.5 (2018-08-09 17:36)
==========
* __DevOps:__ Add support for publishing multiple universes of the same SGT minor version to the public server
* __DevOps:__ Add feature to deploy a game more easily to the public server
* __Fix:__ If user tries to send more units, than the one he have, game will crash with a loading img  
![If user tries to send more units, than the one he have, game will crash with a loading img](assets/changelog/bugs/wLpFm1fg.gif)
* __Fix:__ The user can't close the report from a mobile device
* __Fix:__ Attacks are failing sometimes, as game is trying to remove the units more than one time
* __Fix:__ Multiverse doesn't work as expected, the user can't be logged in two different universes

v0.3.4 (2018-07-25 13:06)
==========
* __Improvement:__ Notify user when he doesn't have units in the selected planet
* __Improvement:__ Notify user, that he can't build units, when he/she doesn't meet minimum requirements
* __Fix:__ When the list of "buildable" units is empty, the system is throwing a 500 error, instead of returning a 200, with an empty array
* __Security:__ Backend allows to use a planet that doesn't exists as target for UNIT_BUILD mision
* __Security:__ An user can build units targetting any planet, by sending the planetId
* __Fix:__ Attacks are crashing when multiple units are involved, and in some custom circumstance
* __Fix:__ On some mobile devices the user can't change the selected planet, because he/she can't close the dialog

v0.3.3 (2018-07-15 11:39)
===========
* __Fix:__ When you send all amount you have of one unit, and the units number is above 127, the mission registration would crash...
* __Fix:__ Game displays missions other than "Explore" when the planet is not explored, ONLY explore should be available
* __Security:__ Backend server allows to send missions to unexplored planets
* __Fix:__ Bad repository for unit in default data for table "objects
* __Fix:__ The background image doesn't take all the screen in some devices
* __Improvement:__ Limit number of decimals in numbers 
* __Fix:__ Unit build cancellation is not working, was working in 2.1

v0.3.2 (2018-07-13 15:35)
===========
* __Fix:__ The first time the build unit. build tab is open, the unit been build won't show, have to go to tab "In planet" and back to "Build"
* __Improvement:__ Display a countdown in the missions, instead of termination date
* __Fix:__ After building a unit the Cancel button doesn't get back to build button unless you refresh the webpage
* __Fix:__ Energy should not be displayed in 0.3.x, as it is not really supported
* __Fix:__ Upgrades are not getting cancelled
* __Fix:__ When running in a mobile phone the user can't scroll in the faction selection menu
* __Fix:__ Unit Missions are not working, they are getting stuck
* __Improvement:__ Get frontend version from package.json and display it, also notify user when a new version has been upload 

v0.3.1 (2018-07-05 10:06)
===========
* __Fix:__ Safe domain detection not working in the frontend
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
