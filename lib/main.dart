import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter/services.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Plate Counter',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: PlateScreen(),
    );
  }
}

class PlateScreen extends StatefulWidget {
  @override
  _PlateScreenState createState() => _PlateScreenState();
}

class _PlateScreenState extends State<PlateScreen> {
  String username = ''; // Make username non-nullable
  String plateCount = "Loading...";
  FirebaseFirestore firestore = FirebaseFirestore.instance;

  @override
  void initState() {
    super.initState();
    _checkUsername();
  }

  // Update _checkUsername to send username to widget
  Future<void> _checkUsername() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    String? savedUsername = prefs.getString('username');
    if (savedUsername == null) {
      _askForUsername();
    } else {
      bool usernameExists = await _checkUsernameExists(savedUsername);
      if (usernameExists) {
        username = savedUsername;
        _refreshPlateData();
        _sendUsernameToWidget(username); // Send the username to the widget
      } else {
        _askForUsername();
      }
    }
  }

  // Check if the username exists in Firestore
  Future<bool> _checkUsernameExists(String username) async {
    // Check if the Firestore collection for this username exists
    DocumentSnapshot snapshot =
        await firestore.collection(username).doc('save').get();
    return snapshot.exists;
  }

  // Ask the user for a username and store it locally
  Future<void> _askForUsername() async {
    SharedPreferences prefs = await SharedPreferences.getInstance();
    String? enteredUsername = await _showUsernameDialog();
    if (enteredUsername != null && enteredUsername.isNotEmpty) {
      bool usernameExists = await _checkUsernameExists(enteredUsername);
      if (usernameExists) {
        prefs.setString('username', enteredUsername);
        username = enteredUsername;
        _refreshPlateData();
      } else {
        // Handle the case where the username doesn't exist in Firestore
        _showErrorDialog("Username doesn't exist. Please try again.");
        _askForUsername(); // Prompt the user again for username
      }
    } else {
      // Handle case where username is null or empty
      print("Username not entered.");
    }
  }

  // Dialog to ask for username input
  Future<String?> _showUsernameDialog() {
    TextEditingController controller = TextEditingController();
    return showDialog<String?>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Enter Username'),
          content: TextField(
            controller: controller,
            decoration: InputDecoration(hintText: "Username"),
          ),
          actions: <Widget>[
            TextButton(
              child: Text('OK'),
              onPressed: () {
                Navigator.of(context).pop(controller.text);
              },
            ),
          ],
        );
      },
    );
  }

  // Show an error dialog
  Future<void> _showErrorDialog(String message) {
    return showDialog<void>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('Error'),
          content: Text(message),
          actions: <Widget>[
            TextButton(
              child: Text('OK'),
              onPressed: () {
                Navigator.of(context).pop();
              },
            ),
          ],
        );
      },
    );
  }

  // Replace sendUsernameToWidget with _sendUsernameToWidget
  Future<void> _refreshPlateData() async {
    try {
      DocumentSnapshot snapshot =
          await firestore.collection(username).doc('save').get();
      if (snapshot.exists) {
        String plateData = snapshot['plate_number'];
        List<String> plateParts = plateData.split('/');
        int plate = int.parse(plateParts[0]);

        DateTime dbTime = DateTime.parse(snapshot['timestamp']);
        DateTime now = DateTime.now();

        Duration timeDifference = now.difference(dbTime);
        int timeDifferenceInMinutes = timeDifference.inMinutes;

        int updatedPlateCount = plate + (timeDifferenceInMinutes ~/ 6);
        if (updatedPlateCount > 240) {
          updatedPlateCount = 240;
        }
        setState(() {
          plateCount = updatedPlateCount.toString();
        });

        // Send the username to the widget after refresh
        _sendUsernameToWidget(username); // <-- Fix here
      } else {
        setState(() {
          plateCount = "No data found";
        });
      }
    } catch (e) {
      setState(() {
        plateCount = "Error: $e";
      });
    }
  }

  // Create a method channel
  static const platform = MethodChannel('com.example.wwptm/widget');

  // Send username to the widget
  Future<void> _sendUsernameToWidget(String username) async {
    try {
      await platform.invokeMethod('sendUsername', {"username": username});
    } catch (e) {
      print("Failed to send username: $e");
    }
  }

  // Refresh the plate data when the button is pressed
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Plate Counter'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              'Current Plate Count:',
              style: TextStyle(fontSize: 20),
            ),
            Text(
              plateCount,
              style: TextStyle(fontSize: 40, fontWeight: FontWeight.bold),
            ),
            SizedBox(height: 20),
            ElevatedButton(
              onPressed: _refreshPlateData,
              child: Text('Refresh'),
            ),
          ],
        ),
      ),
    );
  }
}
