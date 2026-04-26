package com.vula.app.auth.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Country(val code: String, val dialCode: String, val name: String, val flag: String)

// A small subset for demonstration, can be expanded later
val commonCountries = listOf(
    Country("US", "+1", "United States", "🇺🇸"),
    Country("GB", "+44", "United Kingdom", "🇬🇧"),
    Country("ZA", "+27", "South Africa", "🇿🇦"),
    Country("IN", "+91", "India", "🇮🇳"),
    Country("NG", "+234", "Nigeria", "🇳🇬"),
    Country("KE", "+254", "Kenya", "🇰🇪"),
    Country("AU", "+61", "Australia", "🇦🇺"),
    Country("CA", "+1", "Canada", "🇨🇦"),
    Country("DE", "+49", "Germany", "🇩🇪"),
    Country("FR", "+33", "France", "🇫🇷"),
    Country("BR", "+55", "Brazil", "🇧🇷"),
    Country("JP", "+81", "Japan", "🇯🇵")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodeSelector(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clickable { showBottomSheet = true }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = selectedCountry.flag, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = selectedCountry.dialCode,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Select Country",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            CountrySelectionSheet(
                countries = commonCountries,
                onCountrySelected = {
                    onCountrySelected(it)
                    showBottomSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountrySelectionSheet(
    countries: List<Country>,
    onCountrySelected: (Country) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) countries
        else countries.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.dialCode.contains(searchQuery)
        }
    }

    Column(modifier = Modifier.fillMaxHeight(0.8f)) {
        Text(
            text = "Select Country",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search country or code") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(filteredCountries) { country ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCountrySelected(country) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = country.flag, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = country.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = country.dialCode,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
            }
        }
    }
}
