package com.example.worktimetracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.worktimetracker.data.local.entity.GeofenceZone
import com.example.worktimetracker.data.local.entity.ZoneType
import com.example.worktimetracker.ui.viewmodel.MapViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(48.1351, 11.5820), // Default: Munich
            12f
        )
    }

    // Handle camera target updates
    LaunchedEffect(uiState.cameraTarget) {
        uiState.cameraTarget?.let { target ->
            cameraPositionState.animate(
                update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(target, 15f),
                durationMs = 1000
            )
            viewModel.clearCameraTarget()
        }
    }

    Scaffold(
        topBar = {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSearch = { viewModel.performSearch(it) },
                searchResults = uiState.searchResults,
                isSearching = uiState.isSearching,
                searchError = uiState.searchError,
                onResultClick = { viewModel.selectSearchResult(it) },
                onClearSearch = { viewModel.clearSearch() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startAddingZone() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Zone")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true
                ),
                onMapClick = { latLng ->
                    if (uiState.isEditingZone) {
                        viewModel.setZonePosition(latLng)
                    }
                }
            ) {
                // Render existing zones
                uiState.zones.forEach { zone ->
                    Marker(
                        state = MarkerState(position = LatLng(zone.latitude, zone.longitude)),
                        title = zone.name,
                        snippet = "${zone.zoneType.name} - ${zone.radiusMeters.toInt()}m",
                        onClick = {
                            viewModel.startEditingZone(zone)
                            false
                        }
                    )

                    Circle(
                        center = LatLng(zone.latitude, zone.longitude),
                        radius = zone.radiusMeters.toDouble(),
                        fillColor = Color(zone.color).copy(alpha = 0.2f),
                        strokeColor = Color(zone.color),
                        strokeWidth = 2f
                    )
                }

                // Render temporary position during editing
                uiState.temporaryPosition?.let { position ->
                    Marker(
                        state = MarkerState(position = position),
                        title = "New Zone"
                    )

                    Circle(
                        center = position,
                        radius = uiState.temporaryRadius.toDouble(),
                        fillColor = Color(uiState.temporaryColor).copy(alpha = 0.2f),
                        strokeColor = Color(uiState.temporaryColor),
                        strokeWidth = 2f
                    )
                }
            }

            // Zone list at bottom
            if (!uiState.isEditingZone) {
                ZoneList(
                    zones = uiState.zones,
                    onEditZone = { viewModel.startEditingZone(it) },
                    onDeleteZone = { viewModel.deleteZone(it) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Edit Zone Bottom Sheet
        if (uiState.isEditingZone) {
            ZoneEditorBottomSheet(
                zone = uiState.selectedZone,
                name = uiState.temporaryName,
                type = uiState.temporaryType,
                radius = uiState.temporaryRadius,
                color = uiState.temporaryColor,
                position = uiState.temporaryPosition,
                onNameChange = { viewModel.setZoneName(it) },
                onTypeChange = { viewModel.setZoneType(it) },
                onRadiusChange = { viewModel.setZoneRadius(it) },
                onColorChange = { viewModel.setZoneColor(it) },
                onSave = { viewModel.saveZone() },
                onCancel = { viewModel.cancelEditing() },
                onDelete = {
                    uiState.selectedZone?.let { viewModel.deleteZone(it) }
                    viewModel.cancelEditing()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<com.example.worktimetracker.domain.SearchResult>,
    isSearching: Boolean,
    searchError: String?,
    onResultClick: (com.example.worktimetracker.domain.SearchResult) -> Unit,
    onClearSearch: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search address...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = onClearSearch) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearch(query) }
                    )
                )
            }
        )

        // Search Results Dropdown
        if (searchResults.isNotEmpty() || isSearching || searchError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                when {
                    isSearching -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    searchError != null -> {
                        Text(
                            text = "Error: $searchError",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    searchResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(searchResults) { result ->
                                SearchResultItem(
                                    result = result,
                                    onClick = { onResultClick(result) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: com.example.worktimetracker.domain.SearchResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = result.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun ZoneList(
    zones: List<GeofenceZone>,
    onEditZone: (GeofenceZone) -> Unit,
    onDeleteZone: (GeofenceZone) -> Unit,
    modifier: Modifier = Modifier
) {
    if (zones.isEmpty()) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn(
            modifier = Modifier.padding(8.dp)
        ) {
            items(zones) { zone ->
                ZoneListItem(
                    zone = zone,
                    onEdit = { onEditZone(zone) },
                    onDelete = { onDeleteZone(zone) }
                )
            }
        }
    }
}

@Composable
private fun ZoneListItem(
    zone: GeofenceZone,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onEdit),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(zone.color), CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(zone.color)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = zone.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${zone.zoneType.name.replace("_", " ")} - ${zone.radiusMeters.toInt()}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Edit")
        }

        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZoneEditorBottomSheet(
    zone: GeofenceZone?,
    name: String,
    type: ZoneType,
    radius: Float,
    color: Int,
    position: LatLng?,
    onNameChange: (String) -> Unit,
    onTypeChange: (ZoneType) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (zone == null) "New Zone" else "Edit Zone",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Type:", style = MaterialTheme.typography.labelLarge)
            ZoneType.values().forEach { zoneType ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTypeChange(zoneType) }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = type == zoneType,
                        onClick = { onTypeChange(zoneType) }
                    )
                    Text(
                        text = zoneType.name.replace("_", " "),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Color:", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0xFFFF0000.toInt(), // Red
                    0xFF0000FF.toInt(), // Blue
                    0xFF00FF00.toInt(), // Green
                    0xFFFFFF00.toInt(), // Yellow
                    0xFFFF00FF.toInt(), // Magenta
                    0xFF00FFFF.toInt()  // Cyan
                ).forEach { colorValue ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(colorValue), CircleShape)
                            .border(
                                BorderStroke(
                                    2.dp,
                                    if (color == colorValue) Color.Black else Color.Transparent
                                ),
                                CircleShape
                            )
                            .clickable { onColorChange(colorValue) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Radius: ${radius.toInt()}m",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = radius,
                onValueChange = onRadiusChange,
                valueRange = 50f..500f,
                modifier = Modifier.fillMaxWidth()
            )

            if (position == null) {
                Text(
                    "Tap on the map to set the location",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (zone != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete")
                    }
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = onSave,
                    enabled = name.isNotBlank() && position != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
