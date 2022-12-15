/*
Developed By Gareth Murfin www.garethmurfin.com
This app demonstates printing to Bluetooth Star Micronics printers from
Flutter. It has only been tested on the SM-T300i Series but most of them
 should work since the code is based on the official SDK.
* */
package com.example.attempt_one 

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.attempt_one.R
import com.starmicronics.stario10.*
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.MagnificationParameter
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.*
import kotlinx.coroutines.*

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        d("onCreate, this app will now detect printer and print something");

        val discoveryButton = findViewById<Button>(R.id.discoveryButton)
        val discoveryIntent = Intent(this, DiscoveryActivity::class.java)
        discoveryButton.setOnClickListener { startActivity(discoveryIntent) }

        val printButton = findViewById<Button>(R.id.printButton)
        val printIntent = Intent(this, PrintingActivity::class.java)
        printButton.setOnClickListener { startActivity(printIntent) }

        val monitorButton = findViewById<Button>(R.id.monitorButton)
        val monitorIntent = Intent(this, MonitorActivity::class.java)
        monitorButton.setOnClickListener { startActivity(monitorIntent) }

        val statusButton = findViewById<Button>(R.id.statusButton)
        val statusIntent = Intent(this, StatusActivity::class.java)
        statusButton.setOnClickListener { startActivity(statusIntent) }

        //Gaz hack it so it does what we need automatically, that is :
        //discovery
        discovery();
        //status
        //monitor--we probably dont need to make dialogs for every event so remove soem ones that get in the way, but things like paper out are good etc
        //print!
    }

    fun showDialog(message : String)
    {
        MyDialogFragment(message).show(supportFragmentManager,"ARSE");
    }

    fun d(message : String)
    {
        Log.d("ARSE", message);
    }

    //DISCOVERY CODE
    private val requestCode = 1000
    private var _manager: StarDeviceDiscoveryManager? = null
    var identifier="<error>";
    fun discovery()
    {
        d("DISCOVERING PRINTER");
        // If you are using Android 12 and targetSdkVersion is 31 or later,
        // you have to request Bluetooth permission (Nearby devices permission) to use the Bluetooth printer.
        // https://developer.android.com/about/versions/12/features/bluetooth-permissions
        requestBluetoothPermission()

        if (!hasBluetoothPermission()) {
            d("PERMISSION ERROR: You have to allow Nearby devices to use the Bluetooth printer.")
            showDialog("PERMISSION ERROR: You have to allow Nearby devices to use the Bluetooth printer.");
            return
        }

        val interfaceTypes = mutableListOf<InterfaceType>()
        //if (this.lanIsEnabled) {
        //    interfaceTypes += InterfaceType.Lan
        //}
        //if (this.bluetoothIsEnabled) {
            interfaceTypes += InterfaceType.Bluetooth
        //}
        //if (this.usbIsEnabled) {
        //    interfaceTypes += InterfaceType.Usb
        //}

        try {
            this._manager?.stopDiscovery()

            _manager = StarDeviceDiscoveryManagerFactory.create(
                interfaceTypes,
                applicationContext
            )
            _manager?.discoveryTime = 10000
            _manager?.callback = object : StarDeviceDiscoveryManager.Callback {
                override fun onPrinterFound(printer: StarPrinter) {
                    //editTextDevices.append("${printer.connectionSettings.interfaceType}:${printer.connectionSettings.identifier}\n")

                    d("Found printer: ${printer.connectionSettings.identifier}.")
                    showDialog("Found printer: ${printer.connectionSettings.identifier}");
                    //GAZ pass this directly into status
                    identifier=printer.connectionSettings.identifier;
                    status(identifier)
                }

                override fun onDiscoveryFinished() {
                    d("Discovery finished.")
                    //showDialog("Discovery finished.");
                }
            }

            _manager?.startDiscovery()
        } catch (e: Exception) {
            d("Error: ${e}")

            showDialog("Discovery Error. "+e.message);
        }
    }

    private fun requestBluetoothPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                ), requestCode
            )
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        return checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }



    //STATUS CODE
    val interfaceType = InterfaceType.Bluetooth;
    private fun status( identifier : String) {
        d("status identifier:"+identifier);
        //val editTextIdentifier = findViewById<EditText>(R.id.editTextIdentifier)
        //val identifier = editTextIdentifier.text.toString()
        //val statusTextView = findViewById<TextView>(R.id.statusTextViewIdentifier)

        //val spinnerInterfaceType = findViewById<Spinner>(R.id.spinnerInterfaceType)



        val settings = StarConnectionSettings(interfaceType, identifier)
        val printer = StarPrinter(settings, applicationContext)

        // If you are using Android 12 and targetSdkVersion is 31 or later,
        // you have to request Bluetooth permission (Nearby devices permission) to use the Bluetooth printer.
        // https://developer.android.com/about/versions/12/features/bluetooth-permissions
        if (interfaceType == InterfaceType.Bluetooth || settings.autoSwitchInterface) {
            if (!hasBluetoothPermission()) {
                d("PERMISSION ERROR: You have to allow Nearby devices to use the Bluetooth printer.")
                showDialog("PERMISSION ERROR: You have to allow Nearby devices to use the Bluetooth printer.");
                return
            }
        }

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)

        scope.launch {
            try {
                printer.openAsync().await()
                val status = printer.getStatusAsync().await()

                d("Has Error: ${status.hasError}")
                d("Paper Empty: ${status.paperEmpty}")
                d("Paper Near Empty: ${status.paperNearEmpty}")
                d( "Cover Open: ${status.coverOpen}")
                d("Drawer Open Close Signal: ${status.drawerOpenCloseSignal}")

                var message = "";
                withContext(Dispatchers.Main) {
                    message += ("Has Error: ${status.hasError}\n")
                    message += ("Paper Empty: ${status.paperEmpty}\n")
                    message += ("Paper Near Empty: ${status.paperNearEmpty}\n")
                    message += ("Cover Open: ${status.coverOpen}\n")
                    message += ("Drawer Open Close Signal: ${status.drawerOpenCloseSignal}\n")
                    message += ("\n")

                    showDialog(message)
                    //gaz start monitoring now
                    startMonitor();
                }
            } catch (e: Exception) {
                d("Error: ${e}")
                showDialog("Error 1: ${e}")
                var message = "";
                withContext(Dispatchers.Main) {
                    message += ("Error: ${e}\n\n")
                    showDialog("Error 2: ${e}")
                }
            } finally {
                printer.closeAsync().await()
            }
        }
    }

    //MONITOR

    private var printer: StarPrinter? = null

    override fun onStop() {
        super.onStop()

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)

        scope.launch {
            printer?.closeAsync()?.await()
        }
    }

    private fun startMonitor() {
        //val editTextIdentifier = findViewById<EditText>(R.id.editTextIdentifier)
        //val identifier = editTextIdentifier.text.toString()
        //val statusTextView = findViewById<TextView>(R.id.statusTextViewIdentifier)

        //val spinnerInterfaceType = findViewById<Spinner>(R.id.spinnerInterfaceType)

        //val interfaceType = //when (spinnerInterfaceType.selectedItem.toString()) {
          //  "LAN" -> InterfaceType.Lan
           // "Bluetooth" -> InterfaceType.Bluetooth
           // "USB" -> InterfaceType.Usb
           // else -> return
       // }

        val settings = StarConnectionSettings(interfaceType, identifier)

        // If you are using Android 12 and targetSdkVersion is 31 or later,
        // you have to request Bluetooth permission (Nearby devices permission) to use the Bluetooth printer.
        // https://developer.android.com/about/versions/12/features/bluetooth-permissions
        if (interfaceType == InterfaceType.Bluetooth || settings.autoSwitchInterface) {
            if (!hasBluetoothPermission()) {
                d("PERMISSION ERROR: You have to allow Nearby devices to use the Bluetooth printer.")
                return
            }
        }

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)

        scope.launch {
            printer = StarPrinter(settings, applicationContext)
            printer?.printerDelegate = object : PrinterDelegate() {
                override fun onCommunicationError(e: StarIO10Exception) {
                    super.onCommunicationError(e)
                    d("Printer: Communication Error")
                    d("${e}")
                    showDialog("Printer: Communication Error. "+e.message);

                    scope.launch(Dispatchers.Main) {
                      //  statusTextView.append("Printer: Communication Error\n")
                      //  statusTextView.append("${e}\n")
                    }
                }

                override fun onReady() {
                    super.onReady()
                    d( "Printer: Ready")
                    showDialog("Printer: Ready");
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Printer: Ready\n")
                    }

                    //gaz ok so we are ready to print
                    print()
                }

                override fun onError() {
                    super.onError()
                    d("Printer: Error")
                    showDialog("Printer: Error");
                    scope.launch(Dispatchers.Main) {
                      //  statusTextView.append("Printer: Error\n")
                    }
                }

                override fun onPaperReady() {
                    super.onPaperReady()
                    d( "Printer: Paper Ready")
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Printer: Paper Ready\n")
                    }
                }

                override fun onPaperNearEmpty() {
                    super.onPaperNearEmpty()
                    d( "Printer: Paper Near Empty")
                    showDialog("Printer: Paper Near Empty");
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Printer: Paper Near Empty\n")
                    }
                }

                override fun onPaperEmpty() {
                    super.onPaperEmpty()
                    d("Printer: Paper Empty")
                    showDialog("Printer: Paper Empty")
                    scope.launch(Dispatchers.Main) {
                        //statusTextView.append("Printer: Paper Empty\n")
                    }
                }

                override fun onCoverOpened() {
                    super.onCoverOpened()
                    d( "Printer: Cover Opened")
                    showDialog("Printer: Cover Opened")
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Printer: Cover Opened\n")
                    }
                }

                override fun onCoverClosed() {
                    super.onCoverClosed()
                    d( "Printer: Cover Closed")
                    showDialog("Printer: Cover Closed")
                    scope.launch(Dispatchers.Main) {
                      //  statusTextView.append("Printer: Cover Closed\n")
                    }
                }
            }

            printer?.drawerDelegate = object : DrawerDelegate() {
                override fun onCommunicationError(e: StarIO10Exception) {
                    super.onCommunicationError(e)
                    d( "Drawer: Communication Error")
                    d("error is -> ${e}")
                    showDialog("Drawer: Communication Error ${e}")
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Drawer: Communication Error\n")
                       // statusTextView.append("${e}\n")
                    }
                }

                override fun onOpenCloseSignalSwitched(openCloseSignal: Boolean) {
                    super.onOpenCloseSignalSwitched(openCloseSignal)
                    d( "Drawer: Open Close Signal Switched: ${openCloseSignal}")
                    showDialog("Drawer: Open Close Signal Switched: ${openCloseSignal}")
                    scope.launch(Dispatchers.Main) {
                        //statusTextView.append("Drawer: Open Close Signal Switched: ${openCloseSignal}\n")
                    }
                }
            }

            printer?.inputDeviceDelegate = object : InputDeviceDelegate() {
                override fun onCommunicationError(e: StarIO10Exception) {
                    super.onCommunicationError(e)
                    d( "Input Device: Communication Error")
                    d( "${e}")
                    showDialog("Input Device: Communication Error")
                    scope.launch(Dispatchers.Main) {
                      //  statusTextView.append("Input Device: Communication Error\n")
                       // statusTextView.append("${e}\n")
                    }
                }

                override fun onConnected() {
                    super.onConnected()
                    d( "Input Device: Connected")
                    showDialog("Input Device: Connected")
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Input Device: Connected\n")
                    }
                }

                override fun onDisconnected() {
                    super.onDisconnected()
                    d( "Input Device: Disconnected")
                    showDialog("Input Device: Disconnected")
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Input Device: Disconnected\n")
                    }
                }

                override fun onDataReceived(data: List<Byte>) {
                    super.onDataReceived(data)
                    d("Input Device: DataReceived ${data}")
                    showDialog("Input Device: DataReceived ${data}")
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Input Device: DataReceived ${data}\n")
                    }
                }
            }

            printer?.displayDelegate = object : DisplayDelegate() {
                override fun onCommunicationError(e: StarIO10Exception) {
                    super.onCommunicationError(e)
                    d( "Display: Communication Error")
                    d( "${e}")
                    showDialog("Display: Communication Error ${e}")
                    scope.launch(Dispatchers.Main) {
                        //statusTextView.append("Display: Communication Error\n")
                       // statusTextView.append("${e}\n")
                    }
                }

                override fun onConnected() {
                    super.onConnected()
                    d( "Display: Connected")
                    showDialog("Display: Connected")
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Display: Connected\n")
                    }
                }

                override fun onDisconnected() {
                    super.onDisconnected()
                    d("Display: Disconnected")
                    showDialog("Display: Disconnected")
                    scope.launch(Dispatchers.Main) {
                       // statusTextView.append("Display: Disconnected\n")
                    }
                }
            }

            try {
                printer?.openAsync()?.await()

                scope.launch(Dispatchers.Main) {
                   // buttonMonitor?.text = "Stop Monitoring"
                   // isMonitoring = true
                }
            } catch (e: Exception) {
                d( "Error: ${e}")
                showDialog("Error b: ${e}")
                scope.launch(Dispatchers.Main) {
                   // statusTextView.append("Error: ${e}\n")
                }
            }
        }
    }
    //PRINT

    private fun print() {
        d("we will print now");
       // val editTextIdentifier = findViewById<EditText>(R.id.editTextIdentifier)
       // val identifier = editTextIdentifier.text.toString().trim()//wow yeh this is important :) gaz.
        d( "identifier is "+identifier);
        //val spinnerInterfaceType = findViewById<Spinner>(R.id.spinnerInterfaceType)
        //val interfaceType = when (spinnerInterfaceType.selectedItem.toString()) {
        //    "LAN" -> InterfaceType.Lan
        //    "Bluetooth" -> InterfaceType.Bluetooth
        //    "USB" -> InterfaceType.Usb
        //    else -> return
       // }

        val settings = StarConnectionSettings(interfaceType, identifier)
        val printer = StarPrinter(settings, applicationContext)

      //  Log.d("TAG", "a type "+spinnerInterfaceType.selectedItem.toString());
        // If you are using Android 12 and targetSdkVersion is 31 or later,
        // you have to request Bluetooth permission (Nearby devices permission) to use the Bluetooth printer.
        // https://developer.android.com/about/versions/12/features/bluetooth-permissions
        if (interfaceType == InterfaceType.Bluetooth || settings.autoSwitchInterface) {
            if (!hasBluetoothPermission()) {
                d("PERMISSION ERROR: You have to allow Nearby devices to use the Bluetooth printer.")
                return
            }
        }
        d("Should print now");
        val logo = BitmapFactory.decodeResource(resources, R.drawable.tynepunklogo)//R.drawable.logo_01)
        val coopersmith1 = BitmapFactory.decodeResource(resources, R.drawable.cs)
        val coopersmith2 = BitmapFactory.decodeResource(resources, R.drawable.cs2)
        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.Default + job)

        scope.launch {
            try {
                val builder = StarXpandCommandBuilder()
                builder.addDocument(
                    DocumentBuilder()
                        .addPrinter(
                            PrinterBuilder()
                                .actionPrintImage(ImageParameter(logo, 406))
                                .styleInternationalCharacter(InternationalCharacterType.Usa)
                                .styleCharacterSpace(0.0)
                                .styleAlignment(Alignment.Center)
                                .actionPrintText(
                                    "Gareth Murfin\n" +
                                            "presents\n" +
                                            "a test receipt.\n" +
                                            "\n"
                                )
                                .styleAlignment(Alignment.Left)
                                .actionPrintText(
                                    "Date:MM/DD/YYYY    Time:HH:MM PM\n" +
                                            "--------------------------------\n" +
                                            "\n"
                                )
                                .add(
                                    PrinterBuilder()
                                        .styleBold(true)
                                        .actionPrintText("SALE\n")
                                )
                                .actionPrintText(
                                    "SKU      Description       Total\n" +
                                            "30066   CHABARS PINT       99.99\n" +
                                            "30063   CANNIBAL HOLOCAUST 29.99\n" +
                                            "30068   COOPERSMITH        29.99\n" +
                                            "30060   PLASTER OF PARIS   49.99\n" +
                                            "30061   STIX MAN OUTFIT    35.99\n" +
                                            "30061   SOUS VIDE EGG      35.99\n" +
                                            "\n" +
                                            "Subtotal                  156.95\n" +
                                            "Tax                         0.00\n" +
                                            "--------------------------------\n"
                                )
                                .actionPrintText("Total     ")
                                .add(
                                    PrinterBuilder()
                                        .styleMagnification(MagnificationParameter(2, 2))
                                        .actionPrintText("   $156.95\n")
                                )
                                .actionPrintText(
                                    "--------------------------------\n" +
                                            "\n" +
                                            "Charge\n" +
                                            "156.95\n" +
                                            "Visa XXXX-XXXX-XXXX-0123\n" +
                                            "\n"
                                )
                                .add(
                                    PrinterBuilder()
                                        .styleInvert(true)
                                        .actionPrintText("Refunds and Exchanges\n")
                                )
                                .actionPrintText("Within ")
                                .add(
                                    PrinterBuilder()
                                        .styleUnderLine(true)
                                        .actionPrintText("30 days")
                                )
                                .actionPrintText(" with receipt\n")
                                .actionPrintText(
                                    "And tags attached\n" +
                                            "\n"
                                )
                                .styleAlignment(Alignment.Center)
                                .actionPrintBarcode(
                                    BarcodeParameter("0123456", BarcodeSymbology.Jan8)
                                        .setBarDots(3)
                                        .setHeight(5.0)
                                        .setPrintHri(true)
                                )
                                .actionFeedLine(1)
                                .actionPrintQRCode(
                                    QRCodeParameter("www.gaz.lol\n")
                                        .setLevel(QRCodeLevel.L)
                                        .setCellSize(8)
                                )
                                .actionCut(CutType.Partial)
                        //gaz
                                .actionPrintImage(ImageParameter(coopersmith1, 506))
                                .actionPrintImage(ImageParameter(coopersmith2, 606))
                        )
                )
                val commands = builder.getCommands()

                printer.openAsync().await()
                printer.printAsync(commands).await()

                d("Printing Success")
            } catch (e: Exception) {
                d("Printing Error:----- ${e}")
            } finally {
                printer.closeAsync().await()
            }
        }
    }
}
