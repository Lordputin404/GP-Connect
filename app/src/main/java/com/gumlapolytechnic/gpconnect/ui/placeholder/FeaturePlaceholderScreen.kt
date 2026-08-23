package com.gumlapolytechnic.gpconnect.ui.placeholder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Domain
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gumlapolytechnic.gpconnect.R

/**
 * Campus features reachable from the Home quick-access grid. Phase 2 shows a
 * titled placeholder for each; the real modules arrive in later phases
 * (Library/Canteen/Facilities — Phase 5, Campus Map — Phase 6, and so on).
 */
enum class CampusFeature(
    val routeArg: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
) {
    LIBRARY("library", R.string.feature_library_title, R.string.feature_library_description, Icons.Outlined.MenuBook),
    CANTEEN("canteen", R.string.feature_canteen_title, R.string.feature_canteen_description, Icons.Outlined.Restaurant),
    FACILITIES("facilities", R.string.feature_facilities_title, R.string.feature_facilities_description, Icons.Outlined.Construction),
    DEPARTMENTS("departments", R.string.feature_departments_title, R.string.feature_departments_description, Icons.Outlined.Domain),
    FACULTY("faculty", R.string.feature_faculty_title, R.string.feature_faculty_description, Icons.Outlined.Groups),
    CAMPUS_MAP("campus-map", R.string.feature_campus_map_title, R.string.feature_campus_map_description, Icons.Outlined.Explore);

    companion object {
        fun fromRouteArg(arg: String?): CampusFeature? = entries.firstOrNull { it.routeArg == arg }
    }
}

/**
 * Generic "coming in a later phase" screen: proper title, short explanation
 * of the future feature, and a top app bar Back action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturePlaceholderScreen(feature: CampusFeature, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(feature.titleRes)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(feature.titleRes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(feature.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = stringResource(R.string.feature_phase_note),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
