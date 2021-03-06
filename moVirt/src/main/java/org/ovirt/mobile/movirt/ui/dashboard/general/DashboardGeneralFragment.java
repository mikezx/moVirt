package org.ovirt.mobile.movirt.ui.dashboard.general;

import android.content.Intent;
import android.database.Cursor;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.ovirt.mobile.movirt.R;
import org.ovirt.mobile.movirt.provider.OVirtContract;
import org.ovirt.mobile.movirt.ui.LoaderFragment;
import org.ovirt.mobile.movirt.ui.MainActivity;
import org.ovirt.mobile.movirt.ui.MainActivity_;
import org.ovirt.mobile.movirt.ui.dashboard.PercentageCircleView;
import org.ovirt.mobile.movirt.ui.dashboard.general.resources.UtilizationResource;
import org.ovirt.mobile.movirt.util.usage.Cores;
import org.ovirt.mobile.movirt.util.usage.MemorySize;
import org.ovirt.mobile.movirt.util.usage.Percentage;
import org.ovirt.mobile.movirt.util.usage.UsageResource;

import java.util.List;

public abstract class DashboardGeneralFragment extends LoaderFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    protected TextView summaryMemoryPercentageCircle;
    protected TextView summaryCpuPercentageCircle;
    protected TextView summaryStoragePercentageCircle;

    protected PercentageCircleView cpuPercentageCircle;
    protected PercentageCircleView memoryPercentageCircle;
    protected PercentageCircleView storagePercentageCircle;

    protected void initLoaders() {
        for (int loader : getLoaders()) {
            getLoaderManager().initLoader(loader, null, this);
        }
    }

    protected abstract int[] getLoaders();

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // do nothing
    }

    @Override
    public void restartLoader() {
        for (int loader : getLoaders()) {
            getLoaderManager().restartLoader(loader, null, this);
        }
    }

    @Override
    public void destroyLoader() {
        for (int loader : getLoaders()) {
            getLoaderManager().destroyLoader(loader);
        }
    }

    protected <T extends OVirtContract.HasCoresPerSocket & OVirtContract.HasSockets & OVirtContract.HasCpuUsage>
    Pair<UtilizationResource, Cores> getCpuUtilization(List<T> entities) {
        Cores allCores = new Cores();
        double usedPercentagesSum = 0;

        for (T entity : entities) {
            Cores entityCores = new Cores(entity);

            usedPercentagesSum += entityCores.getValue() * entity.getCpuUsage();
            allCores.addValue(entityCores);
        }

        // average of all host usages
        Percentage used = new Percentage((long) usedPercentagesSum / (allCores.getValue() == 0 ? 1 : allCores.getValue()));
        Percentage total = new Percentage(100);
        Percentage available = new Percentage(total.getValue() - used.getValue());

        return new Pair<>(new UtilizationResource(used, total, available), allCores);
    }

    protected <T extends OVirtContract.HasMemory> UtilizationResource getMemoryUtilization(List<T> entities) {
        MemorySize total = new MemorySize();
        MemorySize used = new MemorySize();
        MemorySize available;

        for (T entity : entities) {
            total.addValue(entity.getMemorySize());
            used.addValue(entity.getUsedMemorySize());
        }

        available = new MemorySize(total.getValue() - used.getValue());

        return new UtilizationResource(used, total, available);
    }

    protected void renderCpuPercentageCircle(UtilizationResource resource, final StartActivityAction action) {
        if (resource == null) {
            return;
        }

        renderPercentageCircleView(cpuPercentageCircle, resource);
        renderSummary(summaryCpuPercentageCircle, resource);

        cpuPercentageCircle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean nullAction = action == null;
                if (!nullAction && MotionEvent.ACTION_UP == event.getAction() && cpuPercentageCircle.isActivated()) {
                    startMainActivity(action);
                }
                return nullAction;
            }
        });
    }

    protected void renderMemoryPercentageCircle(UtilizationResource resource, final StartActivityAction action) {
        if (resource == null) {
            return;
        }

        renderPercentageCircleView(memoryPercentageCircle, resource);
        renderSummary(summaryMemoryPercentageCircle, resource);

        memoryPercentageCircle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean nullAction = action == null;
                if (!nullAction && MotionEvent.ACTION_UP == event.getAction() && memoryPercentageCircle.isActivated()) {
                    startMainActivity(action);
                }
                return nullAction;
            }
        });
    }

    protected void renderStoragePercentageCircle(UtilizationResource resource, final StartActivityAction action) {
        if (resource == null) {
            return;
        }

        renderPercentageCircleView(storagePercentageCircle, resource);
        renderSummary(summaryStoragePercentageCircle, resource);

        storagePercentageCircle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                boolean nullAction = action == null;
                if (!nullAction && MotionEvent.ACTION_UP == event.getAction() && storagePercentageCircle.isActivated()) {
                    startMainActivity(action);
                }
                return nullAction;
            }
        });
    }

    private void renderPercentageCircleView(PercentageCircleView circleView, UtilizationResource resource) {
        circleView.setMaxResource(resource.getTotal());
        circleView.setUsedResource(resource.getUsed());

        String resourceDescription = resource.getTotal() instanceof MemorySize ?
                getString(R.string.unit_used, resource.getTotal().getReadableUnitAsString()) : getString(R.string.used);
        circleView.setUsedResourceDescription(resourceDescription);
    }

    private void renderSummary(TextView textView, UtilizationResource resource) {
        UsageResource totalResource = resource.getTotal();
        String totalText = totalResource.getReadableValueAsString();
        String totalUnit = totalResource.getReadableUnitAsString();
        String availableText;
        String summary;

        if (totalResource instanceof MemorySize) {
            MemorySize totalMemoryResource = (MemorySize) resource.getTotal();
            MemorySize availableMemoryResource = (MemorySize) resource.getAvailable();

            availableText = availableMemoryResource.getReadableValueAsString(totalMemoryResource.getReadableUnit());
            summary = getString(R.string.summary_mem_available_of, availableText, totalText, totalUnit);
        } else {
            availableText = resource.getAvailable().toString();
            summary = getString(R.string.summary_cpu_available_of, availableText, totalText, totalUnit);
        }

        textView.setText(summary);

        // compute size of the text based on the string length,
        int stringLength = availableText.length() + totalText.length() + totalUnit.length();
        int textLength;

        // auto-resizing hack
        switch (stringLength) {
            // 15 is maximum stringLength
            case 15:
            case 14:
            case 13:
                textLength = 12;
                break;
            case 12:
            case 11:
                textLength = 13;
                break;
            // other lengths can be displayed with default size 14sp
            default:
                textLength = 14;
                break;
        }

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textLength);
    }

    protected void startMainActivity(StartActivityAction action) {
        Intent intent = new Intent(getActivity(), MainActivity_.class);
        intent.setAction(action.getFragment().name());
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(MainActivity.Extras.FRAGMENT.name(), action.getFragment());
        intent.putExtra(MainActivity.Extras.ORDER_BY.name(), action.getOrderBy());
        intent.putExtra(MainActivity.Extras.ORDER.name(), action.getOrder());
        startActivity(intent);
    }
}
