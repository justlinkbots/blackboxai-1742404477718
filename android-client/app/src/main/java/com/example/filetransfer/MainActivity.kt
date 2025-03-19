package com.example.filetransfer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filetransfer.data.Server
import com.example.filetransfer.databinding.ActivityMainBinding
import com.example.filetransfer.network.FileTransferApiClient
import com.example.filetransfer.network.ServerDiscoveryManager
import com.example.filetransfer.ui.ServerAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var serverAdapter: ServerAdapter
    private lateinit var discoveryManager: ServerDiscoveryManager
    private var selectedServer: Server? = null
    private var apiClient: FileTransferApiClient? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionDeniedDialog()
        }
    }

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleFileSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupServerDiscovery()
        setupClickListeners()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        serverAdapter = ServerAdapter { server ->
            selectedServer = server
            apiClient = FileTransferApiClient.create(server.fullAddress)
            binding.selectFileButton.isEnabled = true
        }

        binding.serverList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = serverAdapter
        }
    }

    private fun setupServerDiscovery() {
        discoveryManager = ServerDiscoveryManager(this)
        
        lifecycleScope.launch {
            discoveryManager.servers.collectLatest { servers ->
                serverAdapter.submitList(servers)
            }
        }
    }

    private fun setupClickListeners() {
        binding.selectFileButton.setOnClickListener {
            if (selectedServer == null) {
                showSnackbar(getString(R.string.select_server))
                return@setOnClickListener
            }
            pickFileLauncher.launch("*/*")
        }
    }

    private fun checkPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                discoveryManager.startDiscovery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationaleDialog(permission)
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun handleFileSelection(uri: Uri) {
        val selectedServer = this.selectedServer ?: return
        val apiClient = this.apiClient ?: return

        // Get file from URI
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val file = File(cacheDir, "temp_${System.currentTimeMillis()}")
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }

            // Show progress
            binding.uploadProgress.visibility = View.VISIBLE

            // Upload file
            apiClient.uploadFile(
                file = file,
                onProgress = { progress ->
                    binding.uploadProgress.progress = progress
                },
                onSuccess = { response ->
                    binding.uploadProgress.visibility = View.GONE
                    showSnackbar(getString(R.string.transfer_success))
                    file.delete()
                },
                onError = { error ->
                    binding.uploadProgress.visibility = View.GONE
                    showSnackbar(getString(R.string.transfer_failed))
                    file.delete()
                    Timber.e("Upload failed: $error")
                }
            )
        }
    }

    private fun showPermissionRationaleDialog(permission: String) {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(getString(R.string.permission_required))
            .setPositiveButton(R.string.retry) { _, _ ->
                requestPermissionLauncher.launch(permission)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required)
            .setMessage(getString(R.string.permission_required))
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        discoveryManager.startDiscovery()
    }

    override fun onPause() {
        super.onPause()
        discoveryManager.stopDiscovery()
    }
}