package com.echsylon.system2

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.echsylon.system2.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import com.google.gson.Gson
import java.io.ByteArrayInputStream
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class MainActivity : AppCompatActivity() {
    private companion object {
        private const val PERMISSION_PULL = "com.echsylon.system1.MIGRATE"
        private const val PERMISSION_PULL_CODE = 9834
    }

    private val binding: ActivityMainBinding by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.signatures.text = getSignatures()
        binding.permissions.text = getPermissions()
        binding.message.text = extractMigrateDataFromIntent(intent)
        binding.read.setOnClickListener { onReadClick() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        binding.message.text = extractMigrateDataFromIntent(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_PULL_CODE) {
            val index = permissions.indexOf(PERMISSION_PULL)
            when (grantResults[index]) {
                PackageManager.PERMISSION_GRANTED -> {
                    binding.permissions.text = getPermissions()
                    pullMigrateDataFromSource()
                }
                PackageManager.PERMISSION_DENIED -> {
                    Snackbar.make(binding.root, "Permission to pull migrate package denied", LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onReadClick() {
        when (ContextCompat.checkSelfPermission(this, PERMISSION_PULL)) {
            PackageManager.PERMISSION_GRANTED -> pullMigrateDataFromSource()
            PackageManager.PERMISSION_DENIED -> requestPermission()
        }
    }

    private fun getSignatures(): String {
        val stringBuilder = StringBuilder()
        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo
                .apkContentsSigners
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
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
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

    private fun extractMigrateDataFromIntent(intent: Intent?): String {
        val key = intent?.data?.lastPathSegment
        val json = intent?.getStringExtra(key)
        val message = Gson().fromJson(json, Map::class.java)
        return message?.get("package") as? String ?: ""
    }

    private fun requestPermission() {
        requestPermissions(arrayOf(PERMISSION_PULL), PERMISSION_PULL_CODE)
    }

    private fun pullMigrateDataFromSource() {
        val uri = Uri.parse("content://com.echsylon.system1/message")
        binding.message.text = application.contentResolver
                ?.query(uri, null, null, null)
                ?.use { cursor ->
                    cursor.moveToFirst()
                    cursor.getString(0)
                }
    }
}