package com.example.qrlens

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import java.net.MalformedURLException
import java.net.URL

class QR : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private val PERMISO_CAMARA = 1
    private var scannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scannerView = ZXingScannerView(this)
        setContentView(scannerView)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checarPermiso()){
                //se concedió el permiso
            }else{
                solicitarPermiso()
            }
        }

        scannerView?.setResultHandler(this)
        scannerView?.startCamera()
    }

    private fun solicitarPermiso() {
        ActivityCompat.requestPermissions(this@QR, arrayOf(Manifest.permission.CAMERA),PERMISO_CAMARA)
    }

    private fun checarPermiso(): Boolean {
        return (ContextCompat.checkSelfPermission(this@QR, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    override fun handleResult(p0: Result?) {
        //código QR leído
        val scanResult = p0?.text

        Log.d("QR_LEIDO", scanResult!!)

        if (scanResult.isNotEmpty()){
            if(scanResult.toString().startsWith("MATMSG")){
                Toast.makeText(this, getString(R.string.email), Toast.LENGTH_SHORT).show();
                val i = mailElem(scanResult)
                startActivity(i)
                finish()

            } else if(scanResult.toString().startsWith("BEGIN")){
                Toast.makeText(this, getString(R.string.vcard), Toast.LENGTH_SHORT).show();
                val i = vCardElem(scanResult)
                startActivity(i)
                finish()

            } else if(scanResult.toString().startsWith("SMSTO")){
                Toast.makeText(this, getString(R.string.sms), Toast.LENGTH_SHORT).show();
                val smsElements = scanResult.split(":").toTypedArray()
                val i = Intent(Intent.ACTION_SENDTO)
                i.putExtra("sms_body", smsElements[2])
                i.setData(Uri.parse("smsto:" + smsElements[1]))
                startActivity(i)
                finish()

            } else{
                Toast.makeText(this, scanResult.toString(), Toast.LENGTH_SHORT).show();
                try{
                    val url = URL(scanResult)
                    val i = Intent(Intent.ACTION_VIEW)
                    i.setData(Uri.parse(scanResult))
                    startActivity(i)
                    finish()
                }catch(e: MalformedURLException){
                    AlertDialog.Builder(this@QR)
                        .setTitle(getString(R.string.error))
                        .setMessage(getString(R.string.no_valido))
                        .setPositiveButton(getString(R.string.aceptar), DialogInterface.OnClickListener { dialogInterface, i ->
                            dialogInterface.dismiss()
                            finish()
                        })
                        .create()
                        .show()
                }
            }
        }

    }

    fun mailElem(scanResult: CharSequence): Intent{
        val mailElements = scanResult.split(":").toTypedArray()
        val mail = mailElements[2].split(";").toTypedArray()
        val sub = mailElements[3].split(";").toTypedArray()
        val txt = mailElements[4].split(";").toTypedArray()
        val i = Intent(Intent.ACTION_SENDTO)
        i.setType("*/*")
        i.setData(Uri.parse("mailto:"))
        i.putExtra(Intent.EXTRA_EMAIL, mail)
        i.putExtra(Intent.EXTRA_SUBJECT, sub[0])
        i.putExtra(Intent.EXTRA_TEXT, txt[0])

        return i
    }

    fun vCardElem(scanResult: CharSequence): Intent{
        val vCardElements = scanResult.split("\n").toTypedArray()
        val name = vCardElements[3].split(":").toTypedArray()
        val org = vCardElements[4].split(":").toTypedArray()
        val num = vCardElements[8].split(":").toTypedArray()
        val mail = vCardElements[10].split(":").toTypedArray()
        val i = Intent(Intent.ACTION_INSERT)
        i.setType(ContactsContract.Contacts.CONTENT_TYPE)
        i.putExtra(ContactsContract.Intents.Insert.NAME, name[1])
        i.putExtra(ContactsContract.Intents.Insert.PHONE,num[1])
        i.putExtra(ContactsContract.Intents.Insert.EMAIL,mail[1])
        i.putExtra(ContactsContract.Intents.Insert.COMPANY,org[1])

        return i
    }

    override fun onResume() {
        super.onResume()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checarPermiso()){
                if(scannerView == null){
                    scannerView = ZXingScannerView(this)
                    setContentView(scannerView)
                }

                scannerView?.setResultHandler(this)
                scannerView?.startCamera()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scannerView?.stopCamera()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){

            PERMISO_CAMARA -> {
                if(grantResults.isNotEmpty()){
                    if(grantResults[0]!= PackageManager.PERMISSION_GRANTED){
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                                AlertDialog.Builder(this@QR)
                                    .setTitle(getString(R.string.request))
                                    .setMessage(getString(R.string.req_msg))
                                    .setPositiveButton(getString(R.string.aceptar), DialogInterface.OnClickListener { dialogInterface, i ->
                                        requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISO_CAMARA)
                                    })
                                    .setNegativeButton(getString(R.string.cancelar), DialogInterface.OnClickListener { dialogInterface, i ->
                                        dialogInterface.dismiss()
                                        finish()
                                    })
                                    .create()
                                    .show()
                            }else{
                                Toast.makeText(this@QR, getString(R.string.perm_negado), Toast.LENGTH_LONG).show()
                                finish()
                            }
                        }
                    }
                }
            }

        }
    }
}