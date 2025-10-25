package com.example.apexcity.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.apexcity.R
import com.example.apexcity.data.api.MLApiClient
import com.example.apexcity.databinding.FragmentAiReportBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.*

class AIReportFragment : Fragment() {

    private var _binding: FragmentAiReportBinding? = null
    private val binding get() = _binding!!

    private var selectedImageUri: Uri? = null
    private var detectedTitle: String = ""
    private var detectedDescription: String = ""
    private var detectedCategory: String = ""
    private var currentLocation: Pair<Double, Double>? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoFile: File? = null

    // Camera launcher
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoFile?.let { file ->
                selectedImageUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                processSelectedImage()
            }
        }
    }

    // Gallery launcher
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            processSelectedImage()
        }
    }

    // Camera permission
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // Location permission
    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupUI()
        checkLocationPermission()
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.cameraButton.setOnClickListener {
            checkCameraPermission()
        }

        binding.galleryButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.locationLayout.setEndIconOnClickListener {
            checkLocationPermission()
        }

        binding.correctButton.setOnClickListener {
            // User confirmed AI classification is correct
            binding.locationLayout.visibility = View.VISIBLE
            binding.submitButton.visibility = View.VISIBLE
        }

        binding.wrongButton.setOnClickListener {
            // User said AI classification is wrong - go to manual report
            Toast.makeText(requireContext(), "Redirecting to manual report", Toast.LENGTH_SHORT).show()
            navigateToManualReport()
        }

        binding.submitButton.setOnClickListener {
            submitReport()
        }
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun launchCamera() {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        photoFile = File(requireContext().cacheDir, fileName)
        
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile!!
        )
        
        takePicture.launch(uri)
    }

    private fun processSelectedImage() {
        selectedImageUri?.let { uri ->
            // Display image
            binding.placeholderLayout.visibility = View.GONE
            Glide.with(requireContext())
                .load(uri)
                .into(binding.previewImage)

            // Analyze with ML model
            analyzeImage(uri)
        }
    }

    private fun analyzeImage(uri: Uri) {
        binding.loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Convert URI to File
                val file = createTempFileFromUri(uri)
                
                // Create multipart body
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

                // Call ML API
                val response = MLApiClient.apiService.processImage(body)

                binding.loadingOverlay.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    
                    detectedTitle = result.title
                    detectedDescription = result.description
                    detectedCategory = result.category

                    // Show results
                    binding.resultsCard.visibility = View.VISIBLE
                    binding.detectedTitle.text = detectedTitle
                    binding.detectedDescription.text = detectedDescription
                    binding.detectedCategory.text = detectedCategory

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to analyze image: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Clean up temp file
                file.delete()

            } catch (e: Exception) {
                binding.loadingOverlay.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    "Error analyzing image: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
        }
    }

    private fun createTempFileFromUri(uri: Uri): File {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload", ".jpg", requireContext().cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        return tempFile
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    currentLocation = Pair(it.latitude, it.longitude)
                    
                    // Get address from coordinates
                    try {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)
                        if (addresses?.isNotEmpty() == true) {
                            val address = addresses[0].getAddressLine(0)
                            binding.locationInput.setText(address)
                        }
                    } catch (e: Exception) {
                        binding.locationInput.setText("${it.latitude}, ${it.longitude}")
                    }
                }
            }
        }
    }

    private fun submitReport() {
        val location = binding.locationInput.text.toString().trim()

        if (location.isEmpty()) {
            Toast.makeText(requireContext(), "Please add location", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentLocation == null) {
            Toast.makeText(requireContext(), "Location coordinates not available", Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Submit to backend with:
        // - detectedTitle
        // - detectedDescription
        // - detectedCategory
        // - location
        // - currentLocation (lat, lng)
        // - selectedImageUri

        Toast.makeText(requireContext(), "Report submitted successfully!", Toast.LENGTH_SHORT).show()
        parentFragmentManager.popBackStack()
    }

    private fun navigateToManualReport() {
        val fragment = ManualReportFragment().apply {
            arguments = Bundle().apply {
                putString("preselected_image_uri", selectedImageUri.toString())
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}