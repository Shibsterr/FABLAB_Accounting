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
- [ ] Abillity to add/plan events inside a calendar as an user / or assign tasks as a worker to another worker
- [ ] Add certain screens if somethings missing (No Internet connection, No Equipment to show)
- [ ] Fix login and register not allowing to hop between from a single button place (its one way)
- [ ] More checks for image,code,name for new equipment maker
- [ ] Visually make it look nicer
- [ ] New equipment image upload not working on newer phones (its something to due with permissions as WRITE_EXTERNAL_STORAGE)
- - [ ] Add a way to make it ask for camera permissions (Instead of asking it only on QR code scanner)
- [ ] Figure out what Report,Tasks buttons do exactly (Task's right now open google calendar (its your own meant for workers))

## New Features to Add:

- [x] Image upload to new equipment maker image gets added from the storage in firebase (added both)
- [x] Description writer? In New Equipment maker (added)
- [x] Max stock they choose In New Equipment maker (added crit,min,max)
- [x] Make the buttons auto adjust depending on screen size
- [ ] Add a forgot password button to login screen
- [ ] Add certain screens if somethings missing (No Internet connection, No Equipment to show)
- [ ] Ability to add/plan events inside a calendar as an user / or assign tasks as a worker to another worker
- [ ] Add icons to the home page buttons
- [ ] Add settings (theme changer, password changer/reseter (is the only one that does it))
