package com.gumlapolytechnic.gpconnect.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gumlapolytechnic.gpconnect.GPConnectApplication
import com.gumlapolytechnic.gpconnect.R
import com.gumlapolytechnic.gpconnect.data.model.CanteenCategory
import com.gumlapolytechnic.gpconnect.data.model.CanteenMenuItem
import com.gumlapolytechnic.gpconnect.ui.components.CategoryChip
import com.gumlapolytechnic.gpconnect.ui.components.EmptyState
import com.gumlapolytechnic.gpconnect.ui.components.ErrorState
import com.gumlapolytechnic.gpconnect.ui.components.NoticeCardShimmer

/**
 * Canteen Management for the CANTEEN_ADMIN (and SUPER_ADMIN): the
 * college-wide category list (create/edit via dialog, enable/disable
 * toggle, delete) and the full item list (Add/Edit via the form screen,
 * availability toggle, delete). All writes are validated again by the
 * Firestore rules — any other caller sees an error banner, never a crash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCanteenScreen(
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: AdminCanteenViewModel = viewModel {
        AdminCanteenViewModel(app.container.canteenRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var categoryEditTarget by remember { mutableStateOf<CanteenCategory?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var deleteCategoryTarget by remember { mutableStateOf<CanteenCategory?>(null) }
    var deleteItemTarget by remember { mutableStateOf<CanteenMenuItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_canteen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddItem) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.admin_canteen_add_item_title),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(4) { NoticeCardShimmer() }
                }
            }
            state.isError -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ErrorState(
                        message = stringResource(R.string.canteen_error_body),
                        onRetry = viewModel::retry,
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        if (state.actionFailed) {
                            ActionFailedBanner(onDismiss = viewModel::dismissActionError)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        // ---- categories section ----
                        CategorySectionHeader(
                            onAddCategory = {
                                categoryEditTarget = null
                                showCategoryDialog = true
                            },
                        )
                    }
                    if (state.categories.isEmpty()) {
                        item {
                            EmptyState(
                                title = stringResource(R.string.admin_canteen_no_categories_title),
                                message = stringResource(R.string.admin_canteen_no_categories_body),
                            )
                        }
                    } else {
                        items(state.categories, key = { "category-${it.id}" }) { category ->
                            AdminCategoryCard(
                                category = category,
                                isBusy = category.id in state.busyIds,
                                onEdit = {
                                    categoryEditTarget = category
                                    showCategoryDialog = true
                                },
                                onDelete = { deleteCategoryTarget = category },
                                onToggleEnabled = {
                                    viewModel.setCategoryEnabled(category, !category.enabled)
                                },
                            )
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        // ---- items section ----
                        ItemsSectionHeader(onAddItem = onAddItem)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (state.items.isEmpty()) {
                        item {
                            EmptyState(
                                title = stringResource(R.string.admin_canteen_no_items_title),
                                message = stringResource(R.string.admin_canteen_no_items_body),
                            )
                        }
                    } else {
                        items(state.items, key = { it.id }) { item ->
                            AdminCanteenItemCard(
                                item = item,
                                categoryName = state.categoryNames[item.categoryId].orEmpty(),
                                isBusy = item.id in state.busyIds,
                                onEdit = { onEditItem(item.id) },
                                onDelete = { deleteItemTarget = item },
                                onToggleAvailable = {
                                    viewModel.setItemAvailable(item, !item.isAvailable)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        CategoryEditDialog(
            category = categoryEditTarget,
            existingNames = state.categories.map { it.name },
            onDismiss = { showCategoryDialog = false },
            onSave = { name, displayOrder ->
                viewModel.saveCategory(categoryEditTarget, name, displayOrder)
                showCategoryDialog = false
            },
        )
    }

    if (deleteCategoryTarget != null) {
        val category = deleteCategoryTarget!!
        AlertDialog(
            onDismissRequest = { deleteCategoryTarget = null },
            title = { Text(stringResource(R.string.admin_canteen_delete_category_title)) },
            text = {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        deleteCategoryTarget = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.admin_action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategoryTarget = null }) {
                    Text(stringResource(R.string.admin_action_cancel))
                }
            },
        )
    }

    if (deleteItemTarget != null) {
        val item = deleteItemTarget!!
        AlertDialog(
            onDismissRequest = { deleteItemTarget = null },
            title = { Text(stringResource(R.string.admin_canteen_delete_item_title)) },
            text = {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMenuItem(item)
                        deleteItemTarget = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.admin_action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteItemTarget = null }) {
                    Text(stringResource(R.string.admin_action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ActionFailedBanner(onDismiss: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.admin_canteen_action_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_action_dismiss))
            }
        }
    }
}

@Composable
private fun CategorySectionHeader(onAddCategory: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.admin_canteen_categories_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onAddCategory) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.admin_canteen_add_category))
        }
    }
}

@Composable
private fun ItemsSectionHeader(onAddItem: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.admin_canteen_items_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onAddItem) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.admin_canteen_add_item_title))
        }
    }
}

@Composable
private fun AdminCategoryCard(
    category: CanteenCategory,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (!category.enabled) {
                    Text(
                        text = stringResource(R.string.admin_canteen_disabled),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Switch(
                    checked = category.enabled,
                    onCheckedChange = { onToggleEnabled() },
                    enabled = !isBusy,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onEdit, enabled = !isBusy) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.admin_action_edit))
                }
                TextButton(onClick = onDelete, enabled = !isBusy) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.admin_action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminCanteenItemCard(
    item: CanteenMenuItem,
    categoryName: String,
    isBusy: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleAvailable: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.canteen_price_format, item.price),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.isAvailable) {
                Text(
                    text = stringResource(R.string.canteen_unavailable),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            if (isBusy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.admin_requests_working),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onToggleAvailable) {
                        Text(
                            text = stringResource(
                                if (item.isAvailable) {
                                    R.string.admin_canteen_mark_unavailable
                                } else {
                                    R.string.admin_canteen_mark_available
                                },
                            ),
                        )
                    }
                    TextButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.admin_action_edit))
                    }
                    TextButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.admin_action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Create/edit dialog for one category (the AssignRoleDialog pattern): a
 * name and a display order. Editing seeds the fields from the category.
 */
@Composable
private fun CategoryEditDialog(
    category: CanteenCategory?,
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, displayOrder: Int) -> Unit,
) {
    var name by remember(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var displayOrderText by remember(category?.id) {
        mutableStateOf(category?.displayOrder?.toString().orEmpty())
    }
    val nameBlank = name.isBlank()
    val nameDuplicate = category == null &&
        existingNames.any { it.equals(name.trim(), ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (category == null) {
                        R.string.admin_canteen_add_category
                    } else {
                        R.string.admin_canteen_edit_category
                    },
                ),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.admin_canteen_field_category_name)) },
                    isError = nameBlank || nameDuplicate,
                    supportingText = {
                        when {
                            nameBlank -> Text(stringResource(R.string.canteen_error_name_required))
                            nameDuplicate ->
                                Text(stringResource(R.string.admin_canteen_category_exists))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = displayOrderText,
                    onValueChange = { displayOrderText = it.filter { digit -> digit.isDigit() } },
                    label = { Text(stringResource(R.string.admin_canteen_field_display_order)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(name.trim(), displayOrderText.toIntOrNull() ?: 0)
                },
                enabled = !nameBlank && !nameDuplicate,
            ) {
                Text(stringResource(R.string.admin_form_action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_action_cancel))
            }
        },
    )
}

/**
 * Canteen menu item create/edit form (the AdminBookFormScreen pattern):
 * name, price and availability required; category chosen from the live
 * enabled-category chips — never free text; description optional.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCanteenItemFormScreen(
    editItemId: String?,
    onBack: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as GPConnectApplication
    val viewModel: AdminCanteenItemFormViewModel = viewModel(key = editItemId ?: "create") {
        AdminCanteenItemFormViewModel(app.container.canteenRepository, editItemId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isEditMode) {
                                R.string.admin_canteen_edit_item_title
                            } else {
                                R.string.admin_canteen_add_item_title
                            },
                        ),
                    )
                },
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
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 48.dp),
                    )
                }
            }
            state.notFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    EmptyState(
                        title = stringResource(R.string.canteen_item_not_found_title),
                        message = stringResource(R.string.canteen_item_not_found_body),
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp),
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text(stringResource(R.string.admin_canteen_field_item_name)) },
                        isError = state.nameError,
                        supportingText = {
                            if (state.nameError) {
                                Text(stringResource(R.string.canteen_error_name_required))
                            }
                        },
                        singleLine = true,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.priceText,
                        onValueChange = viewModel::onPriceChange,
                        label = { Text(stringResource(R.string.admin_canteen_field_price)) },
                        isError = state.priceError,
                        supportingText = {
                            if (state.priceError) {
                                Text(stringResource(R.string.canteen_error_price))
                            }
                        },
                        singleLine = true,
                        enabled = !state.isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.categoryError) {
                        Text(
                            text = stringResource(R.string.canteen_error_category_required),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    // Category is selectable only — chips over the live
                    // enabled categories, never a free-text field.
                    state.categories.forEach { category ->
                        CategoryChip(
                            label = category.name,
                            selected = state.categoryId == category.id,
                            onClick = { viewModel.onCategoryChange(category.id) },
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (state.categories.isEmpty()) {
                        Text(
                            text = stringResource(R.string.admin_canteen_no_categories_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.admin_canteen_field_available),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Switch(
                            checked = state.isAvailable,
                            onCheckedChange = viewModel::onAvailableChange,
                            enabled = !state.isSaving,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text(stringResource(R.string.admin_canteen_field_description)) },
                        minLines = 2,
                        enabled = !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.displayOrderText,
                        onValueChange = viewModel::onDisplayOrderChange,
                        label = { Text(stringResource(R.string.admin_canteen_field_display_order)) },
                        singleLine = true,
                        enabled = !state.isSaving,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                    if (state.saveError) {
                        Text(
                            text = stringResource(R.string.admin_canteen_save_error),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onBack,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.admin_action_cancel))
                        }
                        Button(
                            onClick = viewModel::save,
                            enabled = !state.isSaving,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.admin_form_saving))
                            } else {
                                Text(stringResource(R.string.admin_form_action_save))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}
