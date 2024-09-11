# Bugs/Things to finish/fix/add:

- [x] Stock button in Home page  (HomeFragment)
	- [x] That opens all the equipments list
- [x] Back button for station equipment list and specific equipment list
	- [x] When clicked it sends back to the previous fragment with a blank screen (Solution could be sending the user to the homefragment upon clicking) (Very simple fixed im ashamed how stupid it was)
- [x] Navigation buttons on side bar showing differently depending on users status (admin,worker,user)
	- [x] Hide certain ones (Tasks,New Equipment)
- [x] QR code scanner that finds a specific equipment
	- [x] Opens the specific fragment equipment layout and gets the details from the database by using the code which gets scanned
- [x] Fix not showing image inside specificequipmentfragment (After picking a equipment from the specific list)
- [x] Abillity to add/plan events inside a calendar as an user / or assign tasks as a worker to another worker
- [x] Fix login and register not allowing to hop between from a single button place (its one way) (bruh)
- [x] More checks for image,code,name for new equipment maker
- [x] New equipment image upload not working on newer phones (its something to due with permissions as WRITE_EXTERNAL_STORAGE)
- [x] Add a way to make it ask for camera permissions (Instead of asking it only on QR code scanner)
- [x] Figure out what Report,Tasks buttons do exactly (Task's right now open google calendar (its your own meant for workers))
- [x] Cut the users knees and add a dropdown list for station and room Nrs

## New Features to Add:

- [x] Image upload to new equipment maker image gets added from the storage in firebase (added both)
- [x] Description writer? In New Equipment maker (added)
- [x] Max stock they choose In New Equipment maker (added crit,min,max)
	- [x] Add the stock page where the worker/admin can add or subtract x amount of stock. The buttons open a popup window where it happens. And it should probably update the stocks when the user adds it)
    - [x] Add checks that prevents the user from adding more then then the max.
    - [x] Upon going below minimum sends the user to gmail and makes them send an email to request new stock
- [x] Make the buttons auto adjust depending on screen size
- [x] Add a forgot password button to login screen
- [ ] Add certain screens if somethings missing (~~No Internet connection~~, ~~No Equipment to show~~)
    - [x] Make it also check ur mobile data. 
    - [ ] Screens that say if there are no tasks also add a loading screen when you open the app to hide the loading data
- [x] Ability to add/plan events inside a calendar as an user / or assign tasks as a worker to another worker
- [ ] Add icons to the home page buttons (Not required)
- [x] Add settings (~~theme changer~~, ~~password changer/reset~~)
  - [ ] Multi language support (Will take the longest as you must add everything inside the strings.xml file so that you can translate it properly)
- [x] Add a sidebar navigation which is only seen by the admin where you can see all the logs displayed.
- [ ] Make sure the layouts are fit to be on many different screen sizes
- [x] Permissions for newer phones may be removed (External storage write is an example) ?
- [ ] Visually make it look nicer (Better colors and such)
- [ ] Update the splashscreen if needed
- [ ] Calendar viewing for normal users to see which days are unavailable and allow them to assing one (Could just store it inside the database)
- [ ] Change whatStat method inside NewEquipmentFragment its not dynamic currently and wont work.


## Improvements:
- [x] Better sorting for logs
- [x] Added a new log entry that shows which user scanned which QR code
- [x] Domain checker for emails when registering
  - [x] Password checker (8 characters, capital letter, symbols)
- [x] Add a QR code checker where it checks if its a valid code otherwise repeat the scan until it scans a correct code (by pattern)
- [x] Date of birth picker during register
- [x] Internet checker for every page
- [x] Multiple languages (en,lv)
- [x] No tasks screen with a refresh button

- [x] Calendar that allows u to "sign up"
- - [x] Make it so that admins and workers can "accept" events before letting them show up as to make sure they arent any overlaps
- - [x] Add more statuses for the events
- - [x] Make them show on the calendar
- [ ] Loading screens
- [ ] Add internet checker for every activity
- [ ] Language changer for stations title, description
- -
- [x] Update the home fragment
- - [x] Update the station showing animation


## What do you need for Firebase:
- [ ] Firebase Storage
	- [ ] Equipment_Icons/
    - [ ] Equipment_instructions/
    - [ ] Station_Icons/
- [ ] Authentication system must use email and password
- [ ] Realtime database
	- [ ] Should create the nodes automatically. If not create them
