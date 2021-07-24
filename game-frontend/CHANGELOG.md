# OWGE changelog

v0.10.2 (latest)
=================
* __Fix:__ [Return missions and unit buildings are failing when terminating at almost the same time #417](https://github.com/KevinGuancheDarias/owge/issues/417)  

v0.10.1 (2021-07-17 17:23)
============================
* [class=Admin] __Improvement:__ [Change time specials, upgrades and units main filter to be the widget one... which works properly #410](https://github.com/KevinGuancheDarias/owge/issues/410)  
* [class=Admin] __Fix:__ [When an improvements error occur in units, upgrades, no error is displayed #415](https://github.com/KevinGuancheDarias/owge/issues/415)  
![When an improvements error occur in units, upgrades, no error is displayed #415](https://user-images.githubusercontent.com/6832422/125193606-29fd0280-e245-11eb-9c35-e229e7395ef6.png)  
* [class=Admin] __Fix:__ [When saving an improvement for unit type, the frontend makes infinite request to the backend #411](https://github.com/KevinGuancheDarias/owge/issues/411)  
* [class=Admin] __Fix:__ [When displaying the modal of an upgrade or an unit, the current type of the unit is not displayed, picture evidence #412](https://github.com/KevinGuancheDarias/owge/issues/412)  
![When displaying the modal of an upgrade or an unit, the current type of the unit is not displayed, picture evidence #412](https://user-images.githubusercontent.com/6832422/125175135-e319f900-e1c1-11eb-834e-834b23a9a4dd.png)
* [class=Admin] __Fix:__ [Displaying the improvement for unit types, won't display to which unit the improvement affect #413](https://github.com/KevinGuancheDarias/owge/issues/413)  
![Displaying the improvement for unit types, won't display to which unit the improvement affect #413](https://user-images.githubusercontent.com/6832422/125175159-1a88a580-e1c2-11eb-90b6-f7fe6e03f7aa.png)  
* [class=Admin] __Fix:__ [Fix all the admin missing literals #414](https://github.com/KevinGuancheDarias/owge/issues/414)  
* __Fix:__ In classic theme fix duplicated planets

v0.10.0 (2021-07-10 20:08)
===========================

* Fix admin panel not allowing to clear cache due to changes produced in #386 solution
* __Feature:__ [As a player I want to have factions with different unit type initial max counts #274](https://github.com/KevinGuancheDarias/owge/issues/274)  
* __Feature:__ [As a player I want to be able to have different gather percentages by primary and secondary for each faction #276](https://github.com/KevinGuancheDarias/owge/issues/276)  
* __Feature:__ [As a player I want to be able to unlock units, upgrades, or time specials when I have built some unit #272](https://github.com/KevinGuancheDarias/owge/issues/272)  
* [class=Admin] __Fix:__ Unit doesn't save when already exists, and has interceptable groups added  
* __Fix:__ Unit information modal close button, doesn't work  
* __Feature:__ [As a player I want to be able to add "critical attack" #259](https://github.com/KevinGuancheDarias/owge/issues/259)  
* [class=Admin] __Feature:__ [As an admin I want to add critical attack to certain unit types or units #266](https://github.com/KevinGuancheDarias/owge/issues/266)  
* [class=Admin] __Fix:__ Unit attack rule not closing afer saving
* __Fix:__ [When an error occur in the sync endpoint, the sync breaks completely, bug due to #386 ....  #390](https://github.com/KevinGuancheDarias/owge/issues/390)  
* __Feature:__ [As a player I want the game to have a multiaccount detection system #380](https://github.com/KevinGuancheDarias/owge/issues/380)  
* __Fix:__ [When a conquest succeed, is not deleting the building mission #389](https://github.com/KevinGuancheDarias/owge/issues/389)  
* __Fix:__ [Conquest are failing when the planet is a special location, and the owner left it before the conquest completed #387](https://github.com/KevinGuancheDarias/owge/issues/387)  
* __Feature:__ [As a player I want to be able to intercept a enemy units with a speed group when I have a unit capable of doing so #9](https://github.com/KevinGuancheDarias/owge/issues/9)  
* __Fix:__ [Sync system is way too unstable, specially on mobile devices #386](https://github.com/KevinGuancheDarias/owge/issues/386)  
* __Fix:__ Changelog not getting generated after removing it from the repo
* [class=Advanced] __Fix:__ Not able to use the custom cors Origin
* __Fix:__ [Conquest are failing when planet has not owner #385](https://github.com/KevinGuancheDarias/owge/issues/385)  
* Merge v0.9.21
* __Feature:__ [As a player I want to have invisible units #11](https://github.com/KevinGuancheDarias/owge/issues/11)  
* __Feature:__ [As a player I want to have units capable of bypassing the unit shield #270](https://github.com/KevinGuancheDarias/owge/issues/270)  
* [class=Admin] __Feature:__ [As an admin I want to be able to define units that can intercept units with a given speed group #21](https://github.com/KevinGuancheDarias/owge/issues/21)  
* [class=Admin] __Feature:__ [As an Admin I want to be able to override the maxCount of a certain unit type for a given faction #273](https://github.com/KevinGuancheDarias/owge/issues/273)  
* [class=Admin] __Feature:__ [As an admin I want to add invisible attribute to some units #23](https://github.com/KevinGuancheDarias/owge/issues/23)  
* [class=Admin] __Feature:__ [As an admin I want to define units that can bypass unit shields #269](https://github.com/KevinGuancheDarias/owge/issues/269)  
* [class=Admin] __Feature:__ [As an admin I want to be able to use the requirement HAVE_UNIT #271](https://github.com/KevinGuancheDarias/owge/issues/271)  
* [class=Admin] __Feature:__ [As an admin I want to be able to define negative bonuses (penalties) #277](https://github.com/KevinGuancheDarias/owge/issues/277)  

v0.9.21 (2021-03-07 20:36)
===========================
* __Fix:__ Random bug in all missions (due to socket message save)
* __Fix:__ Alliance icon doesn't appear sometimes in the sidebar
* __Improvement:__ Add the sponsors interface

v0.9.20 (2021-02-28 21:12)
===========================
* __Fix:__ Fix speed not working properly
* __Fix:__ Conquest no longer fail when the planet doesn't have owner
* __Fix:__ Not able to delete an alliance when there are join requests pending
* __Improvement:__ Improve stability of concurrent events
* __Improvement:__ [Display unit types that a specific unit can attack to #381](https://github.com/KevinGuancheDarias/owge/issues/381)  
* __Fix:__ [Reports are not added automatically under unknown situations #376](https://github.com/KevinGuancheDarias/owge/issues/376)  

v0.9.19 (2021-02-15)
=====================
* __Improvement:__ [Allow to sort ranking by nickname #360](https://github.com/KevinGuancheDarias/owge/issues/360)  
* __Fix:__ [Some missions fails randomly EPIC bug #378](https://github.com/KevinGuancheDarias/owge/issues/378)  
* __Fix:__ [System messages appear in the wrong order #377](https://github.com/KevinGuancheDarias/owge/issues/377)  

v0.9.18 (2021-02-07 14:43)
===========================
* __Improvement:__ Move missions icon above the unit limits in the game sidebar
* [class=Advanced]__Improvement:__ Add strong logging information to ease debugging of random attack failures

v0.9.17 (2021-02-04 19:13)
=============================
* __Fix:__ [Display the improved attributes for units, upgrades and time specials #369](https://github.com/KevinGuancheDarias/owge/issues/369)  
* __Fix:__ [The game server crashes when there are toooooooo many messages to delete in the job #375](https://github.com/KevinGuancheDarias/owge/issues/375)  
* __Fix:__ [When the player generates a lot of reports, the game may slow down (and drain a funny amount of battery) #373](https://github.com/KevinGuancheDarias/owge/issues/373)  
* __Improvement:__ [Display the number of units been built #374](https://github.com/KevinGuancheDarias/owge/issues/374)  

v0.9.16 (2021-01-31 20:05)
============================
* __Fix:__ [Remove messages when they are older than 15 days, this should be done in the night #367](https://github.com/KevinGuancheDarias/owge/issues/367)  
* __Fix:__ [Prompt the user to confirm when cancelling unit build #372](https://github.com/KevinGuancheDarias/owge/issues/372)  
* __Fix:__ [When a unit is been built, for stops been unlocked, for any reason, maybe time special expired, or special location was left, the pending time is no displayed #370](https://github.com/KevinGuancheDarias/owge/issues/370)  
* __Improvement:__ [Allow to send system messages #368](https://github.com/KevinGuancheDarias/owge/issues/368)  

v0.9.15 (2020-12-22 19:21)
===========================
* __Fix:__ [Missing fast explore icon in planet list #359](https://github.com/KevinGuancheDarias/owge/issues/359)  
* __Fix:__ [Reports are not marked as read, if the information comes from a socket event #366](https://github.com/KevinGuancheDarias/owge/issues/366)  
* __Fix:__ [Improvements are bugged in all sources, including upgrades, units, and time specials #363](https://github.com/KevinGuancheDarias/owge/issues/363)  
* __Improvement:__ [Allow to disable the alliances #365](https://github.com/KevinGuancheDarias/owge/issues/365)  

v0.9.14 (2020-12-05 17:26)
==========================
* __Improvement:__ [Allow to resize galaxies if world is empty, note, is not perfect #362](https://github.com/KevinGuancheDarias/owge/issues/362)  
* __Fix:__ [Allow to limit alliances by % of world players, and max number configurable in admin panel #358](https://github.com/KevinGuancheDarias/owge/issues/358)  
* __Fix:__ Reduce random errors in attacks
* __Fix:__ [Removing negative units allows to add units without having to wait, nor losing resources #361](https://github.com/KevinGuancheDarias/owge/issues/361)  

v0.9.13 (2020-11-15 13:33)
===========================
* __Improvement:__ [Change improvement formula for upgrades and units, so it's reasonable stay collecting specials #355](https://github.com/KevinGuancheDarias/owge/issues/355)  
* __Fix:__ [Not able to create a new account #354](https://github.com/KevinGuancheDarias/owge/issues/354)
* __Fix:__ [Sometimes the ships go to the same speed than the soldiers #351](https://github.com/KevinGuancheDarias/owge/issues/351)  
* __Fix:__ [Speed sometimes doesn't work as expected, longt times are displayed #353](https://github.com/KevinGuancheDarias/owge/issues/353)  
* __Fix:__ [Deployments stopped working due to performance improvement #352](https://github.com/KevinGuancheDarias/owge/issues/352)  

v0.9.12 (2020-11-10 19:59)
===========================
* __Improvement:__ [Display a upgrade button in the unit requirements screen #338](https://github.com/KevinGuancheDarias/owge/issues/338)  
* __Fix:__ [No unlocked units, and no units in planet string is not multi language #350](https://github.com/KevinGuancheDarias/owge/issues/350)  
* __Fix:__ [Conquest and Eb missions fail sometimes #349](https://github.com/KevinGuancheDarias/owge/issues/349)  
* [class=Advanced]__Improvement:__ Greatly improve database queries performance
* __Improvement:__ Reduce bandwith usage, reduce sync time, and reduce consumed battery

v0.9.11 (2020-11-08 21:24)
===========================
* __Fix:__ [Cross-galaxies is not working properly, wrong behavior #337](https://github.com/KevinGuancheDarias/owge/issues/337)  (Visual only) 
* __Fix:__ [Mission report doesn't display that there is not units, instead is shown as empty #347](https://github.com/KevinGuancheDarias/owge/issues/347)  
* __Fix:__ [Not able to leave planets #348](https://github.com/KevinGuancheDarias/owge/issues/348)  
v0.9.10 (2020-11-08 19:10)
===========================
* __Improvement:__ [Distinct between the units of allies and enemies #340](https://github.com/KevinGuancheDarias/owge/issues/340)
* __Improvement:__ [Show the owners of each unit in the exploration report #339](https://github.com/KevinGuancheDarias/owge/issues/339)  
* __Fix:__ [When there are too many missions in the screen, it displays wrongly in desktop mode #341](https://github.com/KevinGuancheDarias/owge/issues/341)  
* __Improvement:__ [Allow to send conquest even if your max planets limit has been reached #342](https://github.com/KevinGuancheDarias/owge/issues/342)  
* __Fix:__ [Conquest fails when there are no survivors from the original conquest mission #346](https://github.com/KevinGuancheDarias/owge/issues/346)
* __Improvement:__ [Allow to leave a planet even if missions are going/returning, only disallow if there is a build mission in the planet #343](https://github.com/KevinGuancheDarias/owge/issues/343)  
* __Fix:__ [Cross-galaxies is not working properly, wrong behavior #337](https://github.com/KevinGuancheDarias/owge/issues/337)  
* __Fix:__ [Special locations should not display their names in the explore mission, unless it's already explored #336](https://github.com/KevinGuancheDarias/owge/issues/336)  

v0.9.9 (2020-11-03 18:22)
==========================
* __Fix:__ [The tutorial sometimes displays wrongly. And may block the player #289](https://github.com/KevinGuancheDarias/owge/issues/289)  
* [class=Admin] __Improvement:__ [Allow admins to force run Hang missions #315](https://github.com/KevinGuancheDarias/owge/issues/315)  
* __Fix:__ [When a mission crash, the game is not sincing correctly #335](https://github.com/KevinGuancheDarias/owge/issues/335)  
* __Fix:__ [Attacks to planets that don't belong to any user fails #333](https://github.com/KevinGuancheDarias/owge/issues/333)  
* __Fix:__ [When energy is negative, and the unit doesn't require energy, the backend throws "Unexpected fatal error" when building an unit #334](https://github.com/KevinGuancheDarias/owge/issues/334)  

v0.9.8 (2020-11-02 20:58)
==========================
* __Fix:__ Fix admin panel stopped working again
* __Fix:__ [When upgrading version in a service worker env, devices may take some reloads till they pull the changes #313](https://github.com/KevinGuancheDarias/owge/issues/313)  
* __Fix:__ [If the available energy is negative (maybe because a time special is over) the game displays that there are not enough resources even for units or upgrades that don't need energy #327](https://github.com/KevinGuancheDarias/owge/issues/327)  
* __Fix:__ [The time when the upgrade or build bonus is too high displays wrongly, while it work as expected #323](https://github.com/KevinGuancheDarias/owge/issues/323)  
* __Fix:__ [Conquest to planets owned by players without alliance fails, due to #317 conditions #332](https://github.com/KevinGuancheDarias/owge/issues/332)  
* __Fix:__ [Planet list is not synchronising automatically the owner #324](https://github.com/KevinGuancheDarias/owge/issues/324)  
* __Fix:__ [Read mission reports keep showing up as unread new messages until manually synced #326](https://github.com/KevinGuancheDarias/owge/issues/326)  
* __Fix:__ [Upgrade doesn't end unless you force sync #331](https://github.com/KevinGuancheDarias/owge/issues/331)  

v0.9.7 (2020-11-01 22:50)
===========================
* __Fix:__ [Game should "panic" when sync fails #322] (https://github.com/KevinGuancheDarias/owge/issues/322)
* __Fix:__ [When you delete units that use energy, the energy is not restored unless you force sync #328](https://github.com/KevinGuancheDarias/owge/issues/328)  
* __Fix:__ [Active time specials doesn't show up as active #330](https://github.com/KevinGuancheDarias/owge/issues/330)  
* __Fix:__ [Deployed units are not removed from user planet unless forced sync #329](https://github.com/KevinGuancheDarias/owge/issues/329)  
* __Fix:__ [For conquest missions, owner should not lose his planet when ally units survive #317](https://github.com/KevinGuancheDarias/owge/issues/317)  
* __Improvement:__ [Create an admin option to switch enable/disable battle against enemy units caused by establish base missions #319](https://github.com/KevinGuancheDarias/owge/issues/319)  

v0.9.6 (2020-10-31 20:25)
==========================
* __Fix:__ [Unlimited missions when launched from a deployed mission #321](https://github.com/KevinGuancheDarias/owge/issues/321)  
* __Fix:__ [Research time becomes 0 for all upgrades in unknown conditions (some players consider it related to huge mission traffic on their planets) #318](https://github.com/KevinGuancheDarias/owge/issues/318)  
* __Fix:__ [Unit building time reduces to seconds after a player activates two construction specials (faction special after planet special) #320](https://github.com/KevinGuancheDarias/owge/issues/320)  
* __Fix:__ [Solve Conquestspam by making conquest missions interceptable once they have reached 10% of their progress #316](https://github.com/KevinGuancheDarias/owge/issues/316)  
* __Improvement:__ Greatly improve the sync system, reducing way less battery, server power and user bandwith

v0.9.5 (2020-10-25 18:36)
==========================
* __Fix:__ [Conquest was not removing the unlocked units from special locations #312](https://github.com/KevinGuancheDarias/owge/issues/312)  
* __Improvement:__ [Twitch icon should display that I'm live or not, and maybe reload the fixed video if displayed (due to Twitch bug) #304](https://github.com/KevinGuancheDarias/owge/issues/304)  
* __Improvement:__ [Gather and EB missions should trigger an attack, just like the conquest do, but only if there are enemy units in the planet #303](https://github.com/KevinGuancheDarias/owge/issues/303)  
* [class=Admin] __Fix:__ [Fix admin panel not working in official server #299](https://github.com/KevinGuancheDarias/owge/issues/299)
* __Security:__ [The backend is sending the planet information of an unexplored planet as result to POST explore #306](https://github.com/KevinGuancheDarias/owge/issues/306)  
* __Fix:__ [Upgrades are not displaying the time correctly when reloading #300](https://github.com/KevinGuancheDarias/owge/issues/300)  
* __Fix:__ [Explore missions display units that no longer join fights, due to #298 change #301](https://github.com/KevinGuancheDarias/owge/issues/301)  
* __Fix:__ [Deployments are not visually-updating the units count, if only partial count of the units is killed #302](https://github.com/KevinGuancheDarias/owge/issues/302)  
* __Fix:__ [Attacks were failing when a unit doesn't have shield (created from admin panel) #305](https://github.com/KevinGuancheDarias/owge/issues/305)  

v0.9.4 (2020-10-23 19:55)
==========================
* [class=Admin] __Fix:__ [Fix admin panel not working in official server #299](https://github.com/KevinGuancheDarias/owge/issues/299)  (partial)
* __Improvement:__ [Only deployed units join the attacks, gathers trigger attack, if there are deployed units #298](https://github.com/KevinGuancheDarias/owge/issues/298)  
* __Fix:__ [Speed formula allows missions to take less than the base time #297](https://github.com/KevinGuancheDarias/owge/issues/297)  
* __Fix:__ [Attacks are failing when one gather mission participates in the combact #295](https://github.com/KevinGuancheDarias/owge/issues/295)  
* __Fix:__ [Units death in combact are not removed, they appear as "0" units #296](https://github.com/KevinGuancheDarias/owge/issues/296)  

v0.9.3 (2020-10-22 20:24)
===========================
* __Fix:__ [Units death in combat are not updated in the people's browser #292](https://github.com/KevinGuancheDarias/owge/issues/292)  
* __Fix:__ [Attacks are not working properly, some units, are not dieing while they should #294](https://github.com/KevinGuancheDarias/owge/issues/294)  
* __Fix:__ [Sometimes the upgrades don't update the UnitType's max count, nor max energy, nor max missions #287](https://github.com/KevinGuancheDarias/owge/issues/287)  
* __Fix:__ [The attack is not working properly when one unit A can't destroy unit B, should be able to kill unit C, but as unit B can't be attacked, it fails so hard #291](https://github.com/KevinGuancheDarias/owge/issues/291)  
* __Fix:__ [The tutorial sometimes displays wrongly. And may block the player #289](https://github.com/KevinGuancheDarias/owge/issues/289)  (partial fix, keeps failing)

v0.9.2 (2020-10-18 18:56)
===========================
* __Fix:__ [Active time specials time is not properly syncing, when reloading the tab, the time starts from the beginning #286](https://github.com/KevinGuancheDarias/owge/issues/286)  
* __Fix:__ [Conquest or establish base doesn't get deleted, even if all its units has been killed #290](https://github.com/KevinGuancheDarias/owge/issues/290)  

v0.9.1 (2020-10-18 11:32)
===========================
* __Fix:__ [If you deploy multiple unitxs, and in the deployed mission, you send back only some units, the other units will disappear #288](https://github.com/KevinGuancheDarias/owge/issues/288)  
* __Fix:__ [If you deploy to a planet, and then having the units deployed, you send an establish base mission, or a conquest mission, the deployed units will be removed, and will not be usable #285](https://github.com/KevinGuancheDarias/owge/issues/285)  
* __Fix:__ [In the navigation, when a selected galaxy has less sectors and/or quadrants than the previously selected one, should automatically choose sector 1 and/or quadrant 1, to avoid "invisible" navigation #284](https://github.com/KevinGuancheDarias/owge/issues/284)  

v0.9.0 (2020-10-13 17:38)
=================
* __Feature:__ [As a player I want to be able to fast explore by using a single button in the planet #14](https://github.com/KevinGuancheDarias/owge/issues/14)  
* [class=Admin] __Feature:__ [As an admin I want to be able to add "speed bonus" to type of units #20](https://github.com/KevinGuancheDarias/owge/issues/20)  
* __Feature:__ [As an admin I want to be able to choose with type of unit can attack with type of unit #19](https://github.com/KevinGuancheDarias/owge/issues/19)  
* [class=Admin] __Feature:__ [As an admin I want to add interactive tutorials for the game sections #263](https://github.com/KevinGuancheDarias/owge/issues/263)  
* __Feature:__ [As a player I want to have interactive tutorials #264](https://github.com/KevinGuancheDarias/owge/issues/264)  
* [class=Admin] __Feature:__ Allow to specify how much planets for each quadrant should a new galaxy have  
* [class=Admin] __Feature:__ [As an admin I would like to configure a "speed bonus" that when disponible, would allow travelling cross galaxies #22](https://github.com/KevinGuancheDarias/owge/issues/22)  
* __Feature:__ [As a player I want to be able to choose all units to send to the mission, with a single button, or all of a certain type #16](https://github.com/KevinGuancheDarias/owge/issues/16)  
* __Feature:__ [As a player I dont want to be able to send missions to planets outside my galaxy, unless I have reached the requirements for a specified speed group #12](https://github.com/KevinGuancheDarias/owge/issues/12)
* __Feature:__ [As a player I want to have the concept of units speed (internally known as "speed group") #195](https://github.com/KevinGuancheDarias/owge/issues/195)
* __Feature:__ [As a player I want to be able to create a list of planets #7](https://github.com/KevinGuancheDarias/owge/issues/7)
* __Feature:__ [As a player I want to be able to be sinced with the backend (get events of what's going on in realtime), for example current improvements change, current units count change, and more #124](https://github.com/KevinGuancheDarias/owge/issues/124)
* [class=Admin] __Feature:__ [As an admin I want to have the same admin panel functionalities I had in v0.7.x #185](https://github.com/KevinGuancheDarias/owge/issues/185)
* __Merge:__ v0.8.2

v0.8.2 (2020-03-12 15:24)
=================
* __Fix:__ [Do not allow self deploy #215](https://github.com/KevinGuancheDarias/owge/issues/215)
* __Fix:__ [Units that are not in the planet are participating in attacks #216](https://github.com/KevinGuancheDarias/owge/issues/216)
* __Fix:__ [Not able to edit an existing alliance name or description #210](https://github.com/KevinGuancheDarias/owge/issues/210)
* __Improvement:__ [Allow alliance description line breaks to be respected #203](https://github.com/KevinGuancheDarias/owge/issues/203)
* __Fix:__ [When using Chrome language translations, the game hangs #204](https://github.com/KevinGuancheDarias/owge/issues/204)
* __Fix:__ [Some attack missions are failling due to presence of heroes, because game is using legacy improvement system #206](https://github.com/KevinGuancheDarias/owge/issues/206)

v0.8.1 (2020-02-24 11:33)
=================
* __Fix:__ [Countdown in units build is not displaying #197](https://github.com/KevinGuancheDarias/owge/issues/197)
* __Fix:__ [The "Join alliance" doesn't get disabled, when you have already requested to join that alliance #166](https://github.com/KevinGuancheDarias/owge/issues/166)
* __Improvement:__ For max units and max energy use the classic "step-based" percentage computation
* __Improvement:__ In Alliances, style out the slected section button 
* __Fix:__ AOT compilation stopped working
* __Fix:__ [The countdown is not properly syncing, when browser or server has wrong dates #181](https://github.com/KevinGuancheDarias/owge/issues/181)
* __Improvement:__ Display a loading image, when the universe list is not available
* __Fix:__ [Does not allow to see mission types when multiple units are available unless you refresh the browser window #160](https://github.com/KevinGuancheDarias/owge/issues/160)
* __Fix:__ [Upgrade level doesn't get upgrade unless you change refresh browser, or move outside upgrades list and come back #165](https://github.com/KevinGuancheDarias/owge/issues/165)
* __Fix:__ [When you click in alliances the main menu title disappears #167](https://github.com/KevinGuancheDarias/owge/issues/167)
* __Fix:__ [If you open the cancel upgrade level up, and the upgrade finish leveling up before you close, dialog won't work, won't even close #171](https://github.com/KevinGuancheDarias/owge/issues/171)
* __Fix:__ [In spanish translation of "Exploration mission" report, remove extra "n" char #172](https://github.com/KevinGuancheDarias/owge/issues/172)
* __Fix:__ [Mission limit reached does not display its translation #162](https://github.com/KevinGuancheDarias/owge/issues/162)
* __Fix:__ [No resources literal is not translated in units #163](https://github.com/KevinGuancheDarias/owge/issues/163)
* __Fix:__ [In requirements do not show the hide descriptions checkbox #161](https://github.com/KevinGuancheDarias/owge/issues/161)
* [class=Developer] __Fix:__ Account console doesn't work in Linux env
* __Fix:__ [Section title in the menu should not scale when the width is long enough, even when the height is of mobile style #164](https://github.com/KevinGuancheDarias/owge/issues/164)
* __Fix:__ [After the player chooses faction, the modal does not disappear #159](https://github.com/KevinGuancheDarias/owge/issues/159)
* [class=Developer] __Fix:__ OWGE Launcher script for developers, not working in Linux
* __Fix:__ [Planet list icon is in wrong location after certain actions #157](https://github.com/KevinGuancheDarias/owge/issues/157)
* __Fix:__ Battery drainining bug in build units, when the player reloads the browser window, been in the unit build screen
* [class=Advanced] __Improvement:__ The u1 classic export tool now provides the energy icon by itself
* __Fix:__ If you do a deploy mission till you have more than 1 max mission, won't be able to do anything more
* __Improvement:__ Add all the missing language translations to spanish and english (remove all hardcoded texts)
* __Fix:__ Not displaying the "No enough resources" button when you have not enough resources to level up the specified upgrade
* [class=Advanced] __Imprvement:__ The u1 classic import tool, now imports old game time specials
* [class=Admin] __Fix:__ Admin Time specials stopped working for unknown reasons after upgrading
* __Fix:__ Not hiding sidebar when login out
* [class=Advanced] __Fix:__ When universe has not login domain, modal was not closing
* __Improvement:__ Improve time specials view
* __Improvement:__ Improve alliances view
* __Improvement:__ Improve reports view
* __Improvement:__ Improve Unit requirements view
* __Improvement:__ Improve Units, in planet and build views
* __Fix:__ Current section text disappears when you click in a subsection of units section
* __Improvement:__ Improve Upgrades view
* __Fix:__ [When phone is in landscape mode, the modals don't have correct length #156](https://github.com/KevinGuancheDarias/owge/issues/156)
* __Improvement:__ Improve Home view
* __Improvement/Fix:__ In v0.8.0 the sidebar was not working properly in mobile, fix it, and change the way I display it, as not working neither in some laptop scren resolutions
* [class=Developer] __Improvement:__ No longer use SQS as it is unstable on stressful server
* [class=Developer] __Improvement:__ Display slow method executions as warning in the log
* [class=Developer] __Improvement:__ [In unitType/finAll don't compute userBuilt on unitTypes that don't have a maxCount #125](https://github.com/KevinGuancheDarias/owge/issues/125)
* [class=Developer] __Improvement:__ [In mission list don't create instances of display-quadrant if modal is not shown #138](https://github.com/KevinGuancheDarias/owge/issues/138)
* __Improvement:__ [Improve phone battery consumption and performance when browsing the universe #139](https://github.com/KevinGuancheDarias/owge/issues/139)
* __Improvement:__ Doesn't make much sense to display "Involved units" in the attack mission report, remove it 
* __Fix:__ [The units that go in the attack mission, are not participating in the attack itself #149](https://github.com/KevinGuancheDarias/owge/issues/149)
* __Fix:__ [Units deployed in other planets appear in the fight #95](https://github.com/KevinGuancheDarias/owge/issues/95)
* __Fix:__ [Base establishment mission is participating in an attack to the source planet, and not to the target planet #54](https://github.com/KevinGuancheDarias/owge/issues/54)
* __Fix:__ [Displaying units in planet is not working in some cases, when the user owns multiple planets #56](https://github.com/KevinGuancheDarias/owge/issues/56)
* __Fix:__ [You can't see allyes units in exploration report #55](https://github.com/KevinGuancheDarias/owge/issues/55)
* __Fix:__ [Unit building stopped working in 0.8.0 #146](https://github.com/KevinGuancheDarias/owge/issues/146)
* __Fix:__ [Can't leave base X when a mission from base Y to base X is in return state #144](https://github.com/KevinGuancheDarias/owge/issues/144)
* [class=Admin] __Fix:__ [Admin configuration for mission times, is getting reset each time the backend restarts #141](https://github.com/KevinGuancheDarias/owge/issues/141) 
* [class=Advanced] __Improvement:__ Allow to expose dockerized SQS server, when you want to run your backend manually
* [class=Advanced] __Fix:__ In OWGE Docker Launcher, advanced pro guys profile was not working  
* __Improvement:__ Allow to filter changelog by technical level
* [class=Advanced]__Fix:__ Fix in the login should not do clock-sync init if we are going to change domain

v0.8.0 (2019-11-19 03:44)
=================
* __Feature:__ [As a player I would like to display the current upgrade in the game home page #4](https://github.com/KevinGuancheDarias/owge/issues/4)  
![As a player I would like to display the current upgrade in the game home page #4](assets/changelog/features/4.png)  
* [class=Developer]__Feature:__ [As a developer I would like to have an easier way to apply the improvements #121](https://github.com/KevinGuancheDarias/owge/issues/121)  
![As a developer I would like to have an easier way to apply the improvements #121](assets/changelog/features/121.png)  
* __Feature:__ [As a player I would like to be able to use time specials #3](https://github.com/KevinGuancheDarias/owge/issues/3)
* [class=Admin]__Feature:__ [As an admin I want to be able to create time specials](https://github.com/KevinGuancheDarias/owge/issues/2)
* [class=Advanced] __Feature:__ [As a project owner I want the project to use Angular 8 #106](https://github.com/KevinGuancheDarias/owge/issues/106)
* [class=Developer]__Feature:__ [As a frontend developer I would like to have a dockerized environment to focus in frontend tasks #45](https://github.com/KevinGuancheDarias/owge/issues/45)
* __Merge:__ v0.7.5

v0.7.5 (2019-08-13 16:58)
====================
* __Improvement:__ [Disable websocket when not configured](https://github.com/KevinGuancheDarias/owge/issues/105)  
* __Fix:__  [Fix "delete unit" button is moving to the next line when mouse is over](https://github.com/KevinGuancheDarias/owge/issues/104) ![Fix "delete unit" button is moving to the next line when mouse is over](assets/changelog/bugs/104.png)  
* __Fix:__ [Leaving planet is not working when you have BUILD_UNIT mission in any  of your planets](https://github.com/KevinGuancheDarias/owge/issues/103)  

v0.7.4 (2019-08-10 12:32)
==========================
* __Fix:__ [System is not keeping the source & target planet of depoyed missions, when another mission is run](https://github.com/KevinGuancheDarias/owge/issues/101)  
* __Fix:__ [When sending a deployment mission with multiple unit, would create one separated "deployed" mission for each type](https://github.com/KevinGuancheDarias/owge/issues/100)  
* __Fix:__ [Loading image not displaying in mobile devices](https://github.com/KevinGuancheDarias/owge/issues/50)
* __Fix:__ [When the user is not the owner of the alliance, should not display the "join request list"](https://github.com/KevinGuancheDarias/owge/issues/49)  
* __Fix:__ [ClockService is not working properly, looks like it is adding time always](https://github.com/KevinGuancheDarias/owge/issues/99)  
* __Fix:__ [Return button is not working in deployed missions](https://github.com/KevinGuancheDarias/owge/issues/51)  
* __Fix:__ [Button near the sidebard is not working, as sidebar is using more space than visible](https://github.com/KevinGuancheDarias/owge/issues/97)  
* __Fix:__ [Do not allow to send deployments from a deployment mission (multi-deployment)](https://github.com/KevinGuancheDarias/owge/issues/96)  
* __Fix:__ [Deployed units don't appear in the exploration report](https://github.com/KevinGuancheDarias/owge/issues/98)  
* __Fix:__ When you attack source planet of deploy mission, the attack would affect units deployed in other planets
* __Fix:__ Return button doesn't display properly

v0.7.3 (2019-03-14 14:17)
===========================
* __Fix:__ Upgrade timer is not synchronized with the user machine
* __Fix:__ The attemps system of missions, is not automatically deleting BUILD_UNIT mission after 3 attemps
* __Fix:__ Deleting heroes doesn't work when you have cancelled the build of one hero
* __Fix:__ In some resolutions the player can't see the entire sidebar
* __Fix:__ Attacks are not working when one user with an alliance attacks a user that doesn't have an alliance
* __Fix:__ When deploying multiple units of the same type, is not merging the count of the types, this causes errors, as the system doesn't expect to have two separate counts of the same unit type in the same planet  
![When deploying multiple units of the same type, is not merging the count of the types, this causes errors, as the system doesn't expect to have two separate counts of the same unit type in the same planet](assets/changelog/bugs/scPd4AQN.png)  
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
* __DevOps:__ Add support for publishing multiple universes of the same OWGE minor version to the public server
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
