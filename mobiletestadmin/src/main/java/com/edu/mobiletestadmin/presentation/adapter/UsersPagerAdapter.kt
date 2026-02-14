package com.edu.mobiletestadmin.presentation.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import com.edu.mobiletestadmin.R
import com.edu.mobiletestadmin.databinding.ItemViewPagerUserBinding
import com.edu.mobiletestadmin.presentation.model.UserTypeEnum
import com.edu.mobiletestadmin.utils.addDividerItemDecoration

class UsersPagerAdapter(
    private val studentsAdapter: UsersAdapter,
    private val teachersAdapter: UsersAdapter,
    private val selectionListener: SelectionListener? = null,
    private val retryListener: RetryListener? = null,
    private val bundle: Bundle? = null,
    private val noTeachersMessage: Int,
    private val noStudentsMessage: Int

) : RecyclerView.Adapter<UsersPagerAdapter.UserPageVH>() {

    companion object {
        const val PAGES_COUNT = 2
        const val STUDENTS = 0
        const val TEACHERS = 1
    }


    private var studentsTracker: SelectionTracker<String>? = null
    private var teachersTracker: SelectionTracker<String>? = null

    inner class UserPageVH(private val binding: ItemViewPagerUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            position: Int
        ) {

            binding.viewPagerRv.adapter = when (position) {
                STUDENTS -> studentsAdapter
                TEACHERS -> teachersAdapter
                else -> null
            }
            binding.viewPagerRv.addDividerItemDecoration()

            when (position) {
                STUDENTS -> {
                    studentsTracker = buildSelectionTracker(binding, position, studentsAdapter)
                    studentsAdapter.tracker = studentsTracker
                }
                TEACHERS -> {
                    teachersTracker = buildSelectionTracker(binding, position, teachersAdapter)
                    teachersAdapter.tracker = teachersTracker
                }
            }

            studentsTracker?.onRestoreInstanceState(bundle)
            teachersTracker?.onRestoreInstanceState(bundle)

            studentsTracker?.addObserver(object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    studentsTracker?.let {
                        selectionListener?.getStudentsSelection(it.selection.toList())
                    }
                }
            })

            teachersTracker?.addObserver(object : SelectionTracker.SelectionObserver<String>() {
                override fun onSelectionChanged() {
                    super.onSelectionChanged()
                    teachersTracker?.let {
                        selectionListener?.getTeachersSelection(it.selection.toList())
                    }
                }
            })

            binding.retryButton.setOnClickListener {
                val userType =
                    if (position == STUDENTS) UserTypeEnum.STUDENT else UserTypeEnum.TEACHER
                retryListener?.onRetry(userType)
            }

            val currentState = tabPageState[position] ?: PageDataState.LOADING
            reactToPageState(
                currentState,
                if (position == STUDENTS) UserTypeEnum.STUDENT else UserTypeEnum.TEACHER
            )
        }

        fun reactToPageState(
            pageDataState: PageDataState,
            usersType: UserTypeEnum
        ) {
            when (pageDataState) {
                PageDataState.LOADING -> {
                    binding.viewPagerRv.isVisible = false
                    binding.message.isVisible = false
                    binding.errorMessage.isVisible = false
                    binding.retryButton.isVisible = false
                    binding.loader.isVisible = true
                }
                PageDataState.SUCCESS -> {
                    binding.viewPagerRv.isVisible = true
                    binding.message.isVisible = false
                    binding.errorMessage.isVisible = false
                    binding.retryButton.isVisible = false
                    binding.loader.isVisible = false
                }
                PageDataState.EMPTY -> {
                    binding.viewPagerRv.isVisible = false
                    binding.message.isVisible = true
                    binding.message.text = if (usersType == UserTypeEnum.STUDENT) {
                        binding.root.resources.getString(noStudentsMessage)
                    } else {
                        binding.root.resources.getString(noTeachersMessage)
                    }
                    binding.errorMessage.isVisible = false
                    binding.retryButton.isVisible = false
                    binding.loader.isVisible = false
                }
                PageDataState.ERROR -> {
                    binding.viewPagerRv.isVisible = false
                    binding.message.isVisible = false
                    binding.errorMessage.isVisible = true
                    val errorMsg =
                        tabErrorMessage[UserTypeEnum.getPositionByType(usersType)]
                    binding.errorMessage.text = errorMsg
                        ?: binding.root.resources.getString(R.string.error_loading_data)
                    binding.retryButton.isVisible = true
                    binding.loader.isVisible = false
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserPageVH {

        val viewHolder = UserPageVH(
            ItemViewPagerUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
        holderMap[viewType] = viewHolder
        return viewHolder
    }

    override fun onBindViewHolder(holder: UserPageVH, position: Int) {
        holder.bind(position)
    }


    private val holderMap: HashMap<Int, UserPageVH> = hashMapOf()


    private val tabPageState =
        hashMapOf(STUDENTS to PageDataState.LOADING, TEACHERS to PageDataState.LOADING)
    private val tabErrorMessage = hashMapOf<Int, String?>()

    fun updatePageState(
        dataState: PageDataState,
        usersType: UserTypeEnum,
        errorMessage: String? = null
    ) {
        val position = UserTypeEnum.getPositionByType(usersType)
        tabPageState[position] = dataState
        if (dataState == PageDataState.ERROR) {
            tabErrorMessage[position] = errorMessage
        }
        holderMap[position]?.reactToPageState(dataState, usersType)
    }

    /**
     * Kept for backward compatibility. Delegates to [updatePageState].
     */
    fun updateEmptyState(dataState: PageDataState, usersType: UserTypeEnum) {
        updatePageState(dataState, usersType)
    }

    fun getStudentSelection(): List<String> {
        return studentsTracker?.selection?.toList() ?: emptyList()
    }

    fun getTeachersSelection(): List<String> {
        return teachersTracker?.selection?.toList() ?: emptyList()
    }

    fun buildSelectionTracker(
        binding: ItemViewPagerUserBinding,
        position: Int,
        adapter: UsersAdapter
    ) = SelectionTracker.Builder(
        //1
        "selectionItem$position",
        //2
        binding.viewPagerRv,
        //3
        ItemsKeyProvider(
            adapter
        ),
        ItemsDetailsLookup(binding.viewPagerRv),
        //4
        StorageStrategy.createStringStorage()
    ).withSelectionPredicate(
        //5
        SelectionPredicates.createSelectAnything()
    ).build()

    fun saveInstanceState(outState: Bundle) {
        studentsTracker?.onSaveInstanceState(outState)
        teachersTracker?.onSaveInstanceState(outState)
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> STUDENTS
            else -> TEACHERS
        }
    }

    override fun getItemCount() = PAGES_COUNT

    enum class PageDataState {
        LOADING,
        SUCCESS,
        EMPTY,
        ERROR
    }

    interface SelectionListener {
        fun getStudentsSelection(selectionList: List<String>)
        fun getTeachersSelection(selectionList: List<String>)
    }

    interface RetryListener {
        fun onRetry(userType: UserTypeEnum)
    }
}
