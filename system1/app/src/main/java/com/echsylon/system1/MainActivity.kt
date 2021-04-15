package com.echsylon.system1

import android.content.ContentValues
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageInfo
import android.content.pm.PackageManager.GET_PERMISSIONS
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.echsylon.system1.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.google.gson.Gson
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private companion object {
        private const val PERMISSION_PUSH = "com.echsylon.system2.MIGRATE"
        private const val PERMISSION_PUSH_CODE = 3244
    }

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.signatures.text = getSignatures()
        binding.permissions.text = getPermissions()
        binding.message.text = renderNewMessage()
        binding.render.setOnClickListener { onRenderClick() }
        binding.send.setOnClickListener { onSendClick() }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_PUSH_CODE) {
            val index = permissions.indexOf(PERMISSION_PUSH)
            when (grantResults[index]) {
                PERMISSION_GRANTED -> {
                    binding.permissions.text = getPermissions()
                    sendMessage()
                }
                PERMISSION_DENIED -> {
                    Snackbar.make(binding.root, "Permission to send migrate package denied", LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onSendClick() {
        when (ContextCompat.checkSelfPermission(this, PERMISSION_PUSH)) {
            PERMISSION_GRANTED -> sendMessage()
            PERMISSION_DENIED -> requestPermission()
        }
    }

    private fun onRenderClick() {
        val message = renderNewMessage()
        binding.message.text = message
    }

    private fun getSignatures(): String {
        val stringBuilder = StringBuilder()
        val packageInfo = packageManager.getPackageInfo(packageName, GET_SIGNING_CERTIFICATES)
        packageInfo.signingInfo.apkContentsSigners
                .forEach {
                    try {
                        val certStream = ByteArrayInputStream(it.toByteArray())
                        val certFactory = CertificateFactory.getInstance("X509")
                        val x509Cert = certFactory.generateCertificate(certStream) as X509Certificate
                        stringBuilder.appendLine("${x509Cert.subjectDN} (${x509Cert.serialNumber})")
                    } catch (e: CertificateException) {
                        e.printStackTrace();
                    }
                }

        return stringBuilder.toString().trim()
    }

    private fun getPermissions(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, GET_PERMISSIONS)
        val permissionNames = packageInfo.requestedPermissions ?: return ""
        val permissionFlags = packageInfo.requestedPermissionsFlags ?: return ""
        val stringBuilder = StringBuilder()
        if (permissionNames.size != permissionFlags.size) return ""

        for (i in permissionFlags.indices) {
            if (permissionFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0) {
                stringBuilder.appendLine(permissionNames[i])
            }
        }
        return stringBuilder.toString().trim()
    }

    private fun renderNewMessage(): String {
        val bytes = Random.nextBytes(4)
        val hex = "0x" + bytes.joinToString("") { String.format("%02X", it) }
        val uri = Uri.parse("content://com.echsylon.system1/message")
        val values = ContentValues().apply { put("message", hex) }
        contentResolver.insert(uri, values)
        return hex
    }

    private fun requestPermission() {
        requestPermissions(arrayOf(PERMISSION_PUSH), PERMISSION_PUSH_CODE)
    }

    private fun sendMessage() {
        val uri = Uri.parse("content://com.echsylon.system1/message")
        contentResolver.query(uri, null, null, null)
                ?.use { cursor ->
                    val mime = "application/vnd.echsylon+json"
                    val data = Uri.parse("content://com.echsylon.system1/migrate/package")
                    val text = cursor.apply { moveToFirst() }.getString(0)
                    val json = Gson().toJson(mapOf("package" to text))
                    startActivity(Intent(ACTION_VIEW).setDataAndType(data, mime).putExtra("package", json))
                }
    }
}