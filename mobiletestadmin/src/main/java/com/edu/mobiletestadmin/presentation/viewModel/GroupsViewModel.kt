package com.edu.mobiletestadmin.presentation.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.edu.mobiletestadmin.data.model.ResultFirebase
import com.edu.mobiletestadmin.data.repository.IGroupRepo
import com.edu.mobiletestadmin.presentation.model.ResourceState
import com.edu.mobiletestadmin.presentation.model.UserGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class GroupsViewModel(private val groupRepo: IGroupRepo) : BaseViewModel() {

    private val _groupsLiveData: SingleLiveEvent<ResourceState<List<UserGroup>>> = SingleLiveEvent()
    val groupsLiveData: LiveData<ResourceState<List<UserGroup>>> = _groupsLiveData

    fun getGroups() {
        _groupsLiveData.value = ResourceState.Loading
        viewModelScope.launch {

            getResourceStateFlow {
                groupRepo.getAllGroups()
            }.collectLatest { state ->
                _groupsLiveData.value = when (state) {
                    is ResourceState.Success -> {
                        if (state.data.isEmpty()) ResourceState.Empty else state
                    }
                    else -> state
                }
            }
        }
    }

    fun searchGroups(query: String) {
        viewModelScope.launch {
            when (val response = groupRepo.searchGroups(query)) {
                is ResultFirebase.Success -> {
                    _groupsLiveData.value = ResourceState.Success(response.data)
                }
                is ResultFirebase.Error -> {
                    _groupsLiveData.value = ResourceState.Error(response.error.localizedMessage)
                }
            }
        }
    }

}