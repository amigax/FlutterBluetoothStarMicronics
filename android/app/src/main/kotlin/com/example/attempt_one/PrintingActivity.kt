package com.example.attempt_one

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.example.attempt_one.R
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.MagnificationParameter
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrintingActivity : AppCompatActivity() {

    private val requestCode = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printing)

        Log.d("TAG", "onCreate");

        val button = findViewById<Button>(R.id.buttonPrinting)
       ////RIPPED INTO MENU FILE button.setOnClickListener { onPressPrintButton() }

        // If you are using Android 12 and targetSdkVersion is 31 or later,
        // you have to request Bluetooth permission (Nearby devices permission) to use the Bluetooth printer.
        // https://developer.android.com/about/versions/12/features/bluetooth-permissions
        requestBluetoothPermission()
    }


    private fun requestBluetoothPermission() {
        Log.d("TAG", "requestBluetoothPermission");
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


    private fun onPressPrintButton() {
        Log.d("TAG", "onPressPrintButton");
        val editTextIdentifier = findViewById<EditText>(R.id.editTextIdentifier)
        val identifier = editTextIdentifier.text.toString().trim()//wow yeh this is important :) gaz.
        Log.d("TAG", "identifier is "+identifier);
        val spinnerInterfaceType = findViewById<Spinner>(R.id.spinnerInterfaceType)
        val interfaceType = when (spinnerInterfaceType.selectedItem.toString()) {
            "LAN" -> InterfaceType.Lan
            "Bluetooth" -> InterfaceType.Bluetooth
            "USB" -> InterfaceType.Usb
            else -> return
        }

        val settings = StarConnectionSettings(interfaceType, identifier)
        val printer = StarPrinter(settings, applicationContext)

        Log.d("TAG", "a type "+spinnerInterfaceType.selectedItem.toString());
        // If you are using Android 12 and targetSdkVersion is 31 or later,
        // you have to request Bluetooth permission (Nearby devices permission) to use the Bluetooth printer.
        // https://developer.android.com/about/versions/12/features/bluetooth-permissions
        if (interfaceType == InterfaceType.Bluetooth || settings.autoSwitchInterface) {
            if (!hasBluetoothPermission()) {
                Log.d("Printing", "PERMISSION ERROR: You have to allow Nearby devices to use the Bluetooth printer.")
                return
            }
        }
        Log.d("TAG", "Should print now");
        val logo = BitmapFactory.decodeResource(resources, R.drawable.tynepunklogo)
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

                Log.d("Printing", "Success")
            } catch (e: Exception) {
                Log.d("Printing", "Error:----- ${e}")
            } finally {
                printer.closeAsync().await()
            }
        }
    }
}