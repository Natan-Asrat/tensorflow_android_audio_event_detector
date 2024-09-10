package com.emicasolutions.eventdetector;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TriggerAdapter extends RecyclerView.Adapter<TriggerAdapter.TriggerViewHolder> {

    private final Context context;
    private List<TriggerItem> triggerList;
    private Set<Integer> selectedIndexes;
    private ImageView lastDropdown=null;

    // Constructor
    public TriggerAdapter(Context context, List<TriggerItem> triggerList, List<Integer> selectedIndexes) {
        this.triggerList = triggerList;
        this.context = context;
        this.selectedIndexes = new HashSet<>(selectedIndexes); // Avoid duplicates
    }

    public TriggerAdapter(Context context, List<TriggerItem> triggerList) {
        this.triggerList = triggerList;
        this.context = context;
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
        holder.itemView.setOnClickListener(v -> {
            if(lastDropdown!=null){
                lastDropdown.setVisibility(View.GONE);
            }
            holder.dropdownIcon.setRotation(lastDropdown == holder.triggerImage ? 0 : 180);  // Rotate the dropdown icon if expanded

            if (lastDropdown == holder.triggerImage) {
                holder.triggerImage.setVisibility(View.GONE);  // Hide the image
                lastDropdown = null;

            } else {
                holder.triggerImage.setVisibility(View.VISIBLE); // Show the image
                loadImageFromAssets(String.valueOf(triggerItem.getIndex()), holder.triggerImage);
                lastDropdown = holder.triggerImage;

                // You can load the image here, e.g., using Glide or Picasso, or set a specific image
            }

        });
    }
    private void loadImageFromAssets(String imageId, ImageView imageView) {
        AssetManager assetManager = context.getAssets();

        try {
            // Check if the GIF exists in the assets folder
            InputStream inputStream = assetManager.open("images/" + imageId + ".jpg");
            inputStream.close(); // Close stream if found

            // Construct the path to the GIF in assets
            String imagePath = "file:///android_asset/images/" + imageId + ".jpg";

            // Load the GIF using Glide
            Glide.with(context)
                    .load(imagePath)
                    .into(imageView);
        } catch (IOException e) {
            // Handle the case where the GIF is not found (optional)
            Toast.makeText(context, "Respective image not found! ImageId: " + imageId, Toast.LENGTH_SHORT).show();
        }
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
        ImageView dropdownIcon;
        ImageView triggerImage; // ImageView for the image

        public TriggerViewHolder(@NonNull View itemView) {
            super(itemView);
            triggerName = itemView.findViewById(R.id.trigger_text);
            checkBox = itemView.findViewById(R.id.trigger_checkbox);
            dropdownIcon = itemView.findViewById(R.id.dropdown_icon);
            triggerImage = itemView.findViewById(R.id.trigger_image);
        }
    }
}
