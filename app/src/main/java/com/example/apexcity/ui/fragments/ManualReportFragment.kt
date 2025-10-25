package com.example.apexcity.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apexcity.R
import com.example.apexcity.data.api.RetrofitClient
import com.example.apexcity.databinding.FragmentManualReportBinding
import com.example.apexcity.ui.adapters.ImagePreviewAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.*

class ManualReportFragment : Fragment() {

    private var _binding: FragmentManualReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoFile: File? = null
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImagePreviewAdapter
    private var currentLocation: Pair<Double, Double>? = null

    private val categories = listOf(
        "Administrative & Civic Services",
        "Citizen & Safety",
        "Infrastructure",
        "Public Utilities",
        "Traffic/Transport",
        "Environment & Public Spaces",
        "Other"
    )

    // Camera launcher
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            photoFile?.let { file ->
                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    file
                )
                addImageToList(uri)
            }
        }
    }

    // Gallery launcher (multiple images)
    private val pickImages = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            uris.take(5 - selectedImages.size).forEach { uri ->
                addImageToList(uri)
            }
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
        _binding = FragmentManualReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupUI()
        setupImageRecyclerView()
        checkLocationPermission()

        // Get preselected category if coming from category selection
        arguments?.getString("category")?.let { category ->
            binding.categoryInput.setText(category, false)
            binding.toolbar.title = category
        }

        // Check if there's a preselected image from AI report
        arguments?.getString("preselected_image_uri")?.let { uriString ->
            val uri = Uri.parse(uriString)
            addImageToList(uri)
        }
    }

    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Setup category dropdown
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.categoryInput.setAdapter(adapter)

        binding.cameraCard.setOnClickListener {
            checkCameraPermission()
        }

        binding.galleryCard.setOnClickListener {
            if (selectedImages.size >= 5) {
                Toast.makeText(requireContext(), "Maximum 5 images allowed", Toast.LENGTH_SHORT).show()
            } else {
                pickImages.launch("image/*")
            }
        }

        binding.locationInputLayout.setEndIconOnClickListener {
            checkLocationPermission()
        }

        binding.submitButton.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun setupImageRecyclerView() {
        imageAdapter = ImagePreviewAdapter { uri ->
            // Remove image callback
            selectedImages.remove(uri)
            updateImagesList()
        }
        binding.imagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
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

    private fun addImageToList(uri: Uri) {
        if (selectedImages.size < 5) {
            selectedImages.add(uri)
            updateImagesList()
        } else {
            Toast.makeText(requireContext(), "Maximum 5 images allowed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImagesList() {
        if (selectedImages.isEmpty()) {
            binding.imagesRecyclerView.visibility = View.GONE
        } else {
            binding.imagesRecyclerView.visibility = View.VISIBLE
            imageAdapter.submitList(selectedImages.toList())
        }
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

    private fun validateAndSubmit() {
        val category = binding.categoryInput.text.toString().trim()
        val title = binding.titleInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val location = binding.locationInput.text.toString().trim()

        when {
            category.isEmpty() -> {
                Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show()
                return
            }
            title.isEmpty() -> {
                binding.titleInput.error = "Title is required"
                return
            }
            description.isEmpty() -> {
                binding.descriptionInput.error = "Description is required"
                return
            }
            location.isEmpty() -> {
                binding.locationInput.error = "Location is required"
                return
            }
            currentLocation == null -> {
                Toast.makeText(requireContext(), "Please capture location", Toast.LENGTH_SHORT).show()
                return
            }
            selectedImages.isEmpty() -> {
                Toast.makeText(requireContext(), "Please add at least one image", Toast.LENGTH_SHORT).show()
                return
            }
        }

        submitReport(category, title, description, location)
    }

    private fun submitReport(category: String, title: String, description: String, location: String) {
        binding.submitButton.isEnabled = false
        binding.submitButton.text = "Submitting..."

        lifecycleScope.launch {
            try {
                // Prepare location data
                val locationData = mapOf(
                    "address" to location,
                    "coordinates" to mapOf(
                        "lat" to currentLocation!!.first,
                        "lng" to currentLocation!!.second
                    )
                )
                val locationJson = Gson().toJson(locationData)

                // Prepare image parts
                val imageParts = mutableListOf<MultipartBody.Part>()
                selectedImages.forEach { uri ->
                    try {
                        val file = createTempFileFromUri(uri)
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        val part = MultipartBody.Part.createFormData("images", file.name, requestFile)
                        imageParts.add(part)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Create request bodies
                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val categoryBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
                val locationBody = locationJson.toRequestBody("text/plain".toMediaTypeOrNull())

                // Submit to backend
                val response = RetrofitClient.apiService.createComplaint(
                    titleBody,
                    descriptionBody,
                    categoryBody,
                    locationBody,
                    imageParts
                )

                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Report submitted successfully!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    binding.submitButton.isEnabled = true
                    binding.submitButton.text = "Submit Report"
                    Toast.makeText(
                        requireContext(),
                        "Failed to submit: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                binding.submitButton.isEnabled = true
                binding.submitButton.text = "Submit Report"
                Toast.makeText(
                    requireContext(),
                    "Error: ${e.message}",
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}