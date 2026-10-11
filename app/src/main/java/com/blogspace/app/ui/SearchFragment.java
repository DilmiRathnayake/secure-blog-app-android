package com.blogspace.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.data.SearchQuery;
import com.blogspace.app.databinding.FragmentSearchBinding;
import com.blogspace.app.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches post titles and bodies as the user types (partial, case-insensitive) and lists every
 * matching post, title matches first, with the matched text highlighted.
 */
public class SearchFragment extends Fragment implements MainActivity.Refreshable {

    /** Wait this long after the last keystroke before querying, to avoid a query per letter. */
    private static final long DEBOUNCE_MS = 250;

    private FragmentSearchBinding binding;
    private PostAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::runSearch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override
            public void onPostClicked(@NonNull Post post) {
                Navigation.openPost(requireContext(), post.getId());
            }

            @Override
            public void onPostLongPressed(@NonNull Post post) {
                Navigation.openPost(requireContext(), post.getId());
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
            }
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_search);

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable, DEBOUNCE_MS);
            }
        });
        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacks(searchRunnable);
                runSearch();
                hideKeyboard();
                return true;
            }
            return false;
        });
        showPrompt();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    /** Re-runs the current search (e.g. after returning from editing/deleting a result). */
    @Override
    public void refresh() {
        runSearch();
    }

    private String currentInput() {
        return binding.searchInput.getText() == null ? "" : binding.searchInput.getText().toString();
    }

    private void runSearch() {
        if (binding == null) {
            return;
        }
        final String query = SearchQuery.normalise(currentInput());
        if (query == null) {
            showPrompt();
            return;
        }
        Context context = requireContext().getApplicationContext();
        AppExecutors.disk().execute(() -> {
            List<Post> results = PostRepository.getInstance(context).search(query);
            AppExecutors.main(() -> {
                // Ignore stale results if the user kept typing.
                if (binding == null || !query.equals(SearchQuery.normalise(currentInput()))) {
                    return;
                }
                showResults(query, results);
            });
        });
    }

    private void showPrompt() {
        adapter.setHighlightQuery(null);
        adapter.submitList(new ArrayList<>());
        binding.resultCount.setVisibility(View.GONE);
        binding.empty.getRoot().setVisibility(View.VISIBLE);
        binding.empty.emptyTitle.setText(R.string.search_prompt_title);
        binding.empty.emptyBody.setText(R.string.search_prompt_body);
    }

    private void showResults(@NonNull String query, @NonNull List<Post> results) {
        adapter.setHighlightQuery(query);
        // Highlighting depends on the query, so force a rebind rather than relying on DiffUtil.
        adapter.submitList(null);
        adapter.submitList(results);
        if (results.isEmpty()) {
            binding.resultCount.setVisibility(View.GONE);
            binding.empty.getRoot().setVisibility(View.VISIBLE);
            binding.empty.emptyTitle.setText(R.string.search_no_results_title);
            binding.empty.emptyBody.setText(getString(R.string.search_no_results_body, query));
        } else {
            binding.empty.getRoot().setVisibility(View.GONE);
            binding.resultCount.setVisibility(View.VISIBLE);
            binding.resultCount.setText(getResources().getQuantityString(
                    R.plurals.search_result_count, results.size(), results.size(), query));
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.searchInput.getWindowToken(), 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(searchRunnable);
        binding = null;
    }
}
