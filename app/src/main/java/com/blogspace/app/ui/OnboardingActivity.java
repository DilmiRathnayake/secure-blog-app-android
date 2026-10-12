package com.blogspace.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.blogspace.app.R;
import com.blogspace.app.databinding.ActivityOnboardingBinding;
import com.blogspace.app.databinding.ItemOnboardingPageBinding;
import com.blogspace.app.util.AppPrefs;
import com.blogspace.app.util.UiUtils;

/** Three swipeable introduction pages with Skip, Next and Get started. */
public class OnboardingActivity extends AppCompatActivity {

    /** Set when opened again from Settings, so finishing returns to the app instead of setup. */
    public static final String EXTRA_REPLAY = "replay";

    private static final int[] TITLES = {R.string.onb1_title, R.string.onb2_title, R.string.onb3_title};
    private static final int[] BODIES = {R.string.onb1_body, R.string.onb2_body, R.string.onb3_body};
    private static final int[] IMAGES = {R.drawable.ill_welcome, R.drawable.ill_offline, R.drawable.ill_cloud};

    private ActivityOnboardingBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.getRoot(), true);

        binding.pager.setAdapter(new PageAdapter());
        // Subtle depth effect while swiping between pages.
        binding.pager.setPageTransformer((page, position) -> {
            float abs = Math.abs(position);
            page.setAlpha(1f - abs * 0.6f);
            View image = page.findViewById(R.id.illustration);
            if (image != null) {
                image.setTranslationX(position * page.getWidth() * 0.3f);
                image.setScaleX(1f - abs * 0.15f);
                image.setScaleY(1f - abs * 0.15f);
            }
        });
        buildDots();
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateForPage(position);
            }
        });

        binding.btnSkip.setOnClickListener(v -> finishOnboarding());
        binding.btnNext.setOnClickListener(v -> {
            int current = binding.pager.getCurrentItem();
            if (current < TITLES.length - 1) {
                binding.pager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });
        updateForPage(0);
    }

    private void buildDots() {
        int margin = getResources().getDimensionPixelSize(R.dimen.space_xs);
        for (int i = 0; i < TITLES.length; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageResource(R.drawable.dot_indicator);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(margin, 0, margin, 0);
            final int page = i;
            dot.setOnClickListener(v -> binding.pager.setCurrentItem(page, true));
            dot.setContentDescription(getString(R.string.cd_onboarding_page, i + 1, TITLES.length));
            binding.dots.addView(dot, lp);
        }
    }

    private void updateForPage(int position) {
        for (int i = 0; i < binding.dots.getChildCount(); i++) {
            ImageView dot = (ImageView) binding.dots.getChildAt(i);
            dot.setSelected(i == position);
            // The selected dot is a wider pill: re-read the drawable size and re-measure.
            dot.setImageDrawable(null);
            dot.setImageResource(R.drawable.dot_indicator);
            dot.setSelected(i == position);
            dot.requestLayout();
        }
        boolean last = position == TITLES.length - 1;
        binding.btnNext.setText(last ? R.string.onb_get_started : R.string.onb_next);
        binding.btnSkip.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
    }

    private void finishOnboarding() {
        AppPrefs prefs = new AppPrefs(this);
        prefs.setOnboardingDone(true);
        if (!getIntent().getBooleanExtra(EXTRA_REPLAY, false)) {
            startActivity(new Intent(this,
                    prefs.isProfileDone() ? MainActivity.class : ProfileSetupActivity.class));
        }
        finish();
    }

    private static class PageHolder extends RecyclerView.ViewHolder {
        final ItemOnboardingPageBinding b;

        PageHolder(ItemOnboardingPageBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    private static class PageAdapter extends RecyclerView.Adapter<PageHolder> {
        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new PageHolder(ItemOnboardingPageBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            bind(holder.b, IMAGES[position], TITLES[position], BODIES[position]);
        }

        private static void bind(ItemOnboardingPageBinding b, @DrawableRes int image,
                                 @StringRes int title, @StringRes int body) {
            b.illustration.setImageResource(image);
            b.title.setText(title);
            b.body.setText(body);
        }

        @Override
        public int getItemCount() {
            return TITLES.length;
        }
    }
}
