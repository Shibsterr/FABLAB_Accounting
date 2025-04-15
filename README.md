# Bugs:

- [x] Stock button in Home page  (HomeFragment)
    - [x] That opens all the equipments list
- [x] Back button for station equipment list and specific equipment list
    - [x] When clicked it sends back to the previous fragment with a blank screen (Solution could be
      sending the user to the homefragment upon clicking) (Very simple fixed im ashamed how stupid
      it was)
- [x] Navigation buttons on side bar showing differently depending on users status (
  admin,worker,user)
    - [x] Hide certain ones (Tasks,New Equipment)
- [x] QR code scanner that finds a specific equipment
    - [x] Opens the specific fragment equipment layout and gets the details from the database by
      using the code which gets scanned
- [x] Fix not showing image inside specificequipmentfragment (After picking a equipment from the
  specific list)
- [x] Fix login and register not allowing to hop between from a single button place (its one way)
- [x] New equipment image upload not working on newer phones (its something to due with permissions
  as WRITE_EXTERNAL_STORAGE)

## New things to add / Change:

- [x] Image upload to new equipment maker image gets added from the storage in firebase
- [x] Description field in New Equipment tab
- [x] Max stock they choose In New Equipment maker (added crit,min,max)
    - [x] Add the stock page where the worker/admin can add or subtract x amount of stock. The
      buttons open a popup window where it happens. And it should probably update the stocks when
      the user adds it)
    - [x] Add checks that prevents the user from adding more then then the max.
    - [x] Upon going below minimum sends the user to gmail and makes them send an email to request
      new stock
- [x] Make the buttons auto adjust depending on screen size
- [x] Add a forgot password button to login screen
- [x] Add certain screens if somethings missing (No Internet connection, No Equipment to show)
    - [x] Make it also check ur mobile data.
    - [x] Screens that say if there are no tasks also add a loading screen when you open the app to
      hide the loading data
- [x] Ability to add/plan events inside a calendar as an user / or assign tasks as a worker to
  another worker
- [x] Add icons to the home page buttons
- [x] Add settings (theme changer, password changer/reset)
    - [x] Multi language support (Will take the longest as you must add everything inside the
      strings.xml file so that you can translate it properly)
- [x] Add a sidebar navigation which is only seen by the admin where you can see all the logs
  displayed.
- [x] Make sure the layouts are fit to be on many different screen sizes
- [x] Permissions for newer phones may be removed (External storage write is an example)
- [x] Calendar viewing for normal users to see which days are unavailable and allow them to assign
  one (Could just store it inside the database)
- [x] Change whatStat method inside NewEquipmentFragment its not dynamic currently and wont work.
- [x] Abillity to add/plan events inside a calendar as an user / or assign tasks as a worker to
  another worker