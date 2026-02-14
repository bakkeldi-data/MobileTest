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
            }.collectLatest {
                _groupsLiveData.value = when (it) {
                    is ResourceState.Success -> if (it.data.isEmpty()) ResourceState.Empty else it
                    else -> it
                }
            }
        }
    }

    fun searchGroups(query: String) {
        viewModelScope.launch {
            when (val response = groupRepo.searchGroups(query)) {
                is ResultFirebase.Success -> {
                    _groupsLiveData.value =
                        if (response.data.isEmpty()) ResourceState.Empty else ResourceState.Success(
                            response.data
                        )
                }
                is ResultFirebase.Error -> {
                    _groupsLiveData.value = ResourceState.Error(response.error.localizedMessage)
                }
            }
        }
    }

}