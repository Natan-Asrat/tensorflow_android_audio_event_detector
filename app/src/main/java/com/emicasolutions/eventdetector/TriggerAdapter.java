package com.emicasolutions.eventdetector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TriggerAdapter extends RecyclerView.Adapter<TriggerAdapter.TriggerViewHolder> {

    private List<TriggerItem> triggerList;
    private Set<Integer> selectedIndexes;

    // Constructor
    public TriggerAdapter(List<TriggerItem> triggerList, List<Integer> selectedIndexes) {
        this.triggerList = triggerList;
        this.selectedIndexes = new HashSet<>(selectedIndexes); // Avoid duplicates
    }

    public TriggerAdapter(List<TriggerItem> triggerList) {
        this.triggerList = triggerList;
        this.selectedIndexes = new HashSet<>();
    }

    @NonNull
    @Override
    public TriggerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trigger, parent, false);
        return new TriggerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TriggerViewHolder holder, int position) {
        TriggerItem triggerItem = triggerList.get(position);

        // Set the trigger name
        holder.triggerName.setText(triggerItem.getDisplayName());

        // Set the checkbox state based on whether the trigger is selected
        holder.checkBox.setChecked(selectedIndexes.contains(triggerItem.getIndex()));

        // Set a listener to toggle the selection
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIndexes.add(triggerItem.getIndex());
            } else {
                selectedIndexes.remove(triggerItem.getIndex());
            }
        });
    }

    @Override
    public int getItemCount() {
        return triggerList.size();
    }

    // Method to check the trigger based on its index
    public void checkTrigger(int index) {
        // Find the position of the item with the given index
        for (int i = 0; i < triggerList.size(); i++) {
            if (triggerList.get(i).getIndex() == index) {
                // Mark the trigger as selected and notify the adapter
                selectedIndexes.add(index);
                notifyItemChanged(i); // Notify the adapter to refresh this particular item
                break;
            }
        }
    }

    // Method to get selected trigger indexes
    public List<Integer> getSelectedTriggerIndexes() {
        return new ArrayList<>(selectedIndexes);
    }

    // Method to update the list of triggers (used when filtering)
    public void updateTriggerList(List<TriggerItem> newList) {
        this.triggerList = newList;
        notifyDataSetChanged(); // Notify the adapter to refresh the UI with the new data
    }

    static class TriggerViewHolder extends RecyclerView.ViewHolder {

        TextView triggerName;
        CheckBox checkBox;

        public TriggerViewHolder(@NonNull View itemView) {
            super(itemView);
            triggerName = itemView.findViewById(R.id.trigger_text);
            checkBox = itemView.findViewById(R.id.trigger_checkbox);
        }
    }
}
