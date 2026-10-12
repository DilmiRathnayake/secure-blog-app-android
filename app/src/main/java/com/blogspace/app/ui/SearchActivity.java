package com.blogspace.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.data.SearchQuery;
import com.blogspace.app.databinding.ActivitySearchBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.AppPrefs;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches post titles and bodies as the user types (partial, case-insensitive) and lists every
 * match, title matches first, with the matched text highlighted. Recent searches are remembered
 * and shown as chips.
 */
public class SearchActivity extends AppCompatActivity {

    /** Wait this long after the last keystroke before querying, to avoid a query per letter. */
    private static final long DEBOUNCE_MS = 250;

    private ActivitySearchBinding binding;
    private PostAdapter adapter;
    private AppPrefs prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable searchRunnable = this::runSearch;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.getRoot(), true);
        prefs = new AppPrefs(this);

        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override
            public void onPostClicked(@NonNull Post post) {
                rememberQuery();
                Navigation.openPost(SearchActivity.this, post.getId());
            }

            @Override
            public void onPostLongPressed(@NonNull Post post) {
                onPostClicked(post);
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
            }
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnClear.setOnClickListener(v -> binding.searchInput.setText(""));
        binding.btnClearHistory.setOnClickListener(v -> {
            prefs.clearSearchHistory();
            renderHistory();
        });
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.btnClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                handler.removeCallbacks(searchRunnable);
                handler.postDelayed(searchRunnable, DEBOUNCE_MS);
            }
        });
        binding.searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                handler.removeCallbacks(searchRunnable);
                runSearch();
                rememberQuery();
                hideKeyboard();
                return true;
            }
            return false;
        });

        // Animated entrance: the field slides down and expands.
        binding.field.setScaleX(0.92f);
        binding.field.setAlpha(0f);
        binding.field.animate().scaleX(1f).alpha(1f).setDuration(260).start();
        binding.searchInput.requestFocus();
        renderHistory();
        showPrompt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        runSearch(); // results may have changed after editing/deleting a result
    }

    private String currentInput() {
        return binding.searchInput.getText() == null ? "" : binding.searchInput.getText().toString();
    }

    private void rememberQuery() {
        String q = SearchQuery.normalise(currentInput());
        if (q != null) {
            prefs.addSearch(q);
            renderHistory();
        }
    }

    private void renderHistory() {
        List<String> history = prefs.getSearchHistory();
        binding.historyChips.removeAllViews();
        for (String q : history) {
            Chip chip = new Chip(this, null, com.google.android.material.R.attr.chipStyle);
            chip.setText(q);
            chip.setCheckable(false);
            chip.setChipIconResource(R.drawable.ic_history);
            chip.setChipIconVisible(true);
            chip.setOnClickListener(v -> {
                binding.searchInput.setText(q);
                binding.searchInput.setSelection(q.length());
                hideKeyboard();
            });
            binding.historyChips.addView(chip);
        }
        boolean showHistory = !history.isEmpty() && SearchQuery.normalise(currentInput()) == null;
        binding.historySection.setVisibility(showHistory ? View.VISIBLE : View.GONE);
    }

    private void runSearch() {
        final String query = SearchQuery.normalise(currentInput());
        renderHistory();
        if (query == null) {
            showPrompt();
            return;
        }
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            List<Post> results = PostRepository.getInstance(context).search(query);
            AppExecutors.main(() -> {
                // Ignore stale results if the user kept typing.
                if (isDestroyed() || !query.equals(SearchQuery.normalise(currentInput()))) {
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
        binding.emptyScroll.setVisibility(View.VISIBLE);
        binding.empty.emptyIcon.setImageResource(R.drawable.ill_no_results);
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
            binding.emptyScroll.setVisibility(View.VISIBLE);
            binding.empty.emptyIcon.setImageResource(R.drawable.ill_no_results);
            binding.empty.emptyTitle.setText(R.string.search_no_results_title);
            binding.empty.emptyBody.setText(getString(R.string.search_no_results_body, query));
        } else {
            binding.emptyScroll.setVisibility(View.GONE);
            binding.resultCount.setVisibility(View.VISIBLE);
            binding.resultCount.setText(getResources().getQuantityString(
                    R.plurals.search_result_count, results.size(), results.size(), query));
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = ContextCompat.getSystemService(this, InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(binding.searchInput.getWindowToken(), 0);
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(searchRunnable);
        super.onDestroy();
    }
}
