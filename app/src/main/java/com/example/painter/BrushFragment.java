package com.example.painter;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.example.painter.Adapter.ColorAdapter;
import com.example.painter.Interface.BrushFragmentListener;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;


public class BrushFragment extends BottomSheetDialogFragment implements ColorAdapter.ColorAdapterListener {

    SeekBar seekBar_brush_size, seekBar_brush_color;
    RecyclerView recyclerView_color;
    ToggleButton btn_brush_state;
    ColorAdapter colorAdapter;

    BrushFragmentListener listener;

    static BrushFragment instace;

    public static BrushFragment getInstace() {
        if(instace==null)
            instace = new BrushFragment();
        return instace;
    }

    public BrushFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View itemView = inflater.inflate(R.layout.fragment_brush, container, false);
        seekBar_brush_size = (SeekBar) itemView.findViewById(R.id.seekbar_brush_size);
        seekBar_brush_color = (SeekBar) itemView.findViewById(R.id.seekbar_brush_color);
        btn_brush_state = (ToggleButton) itemView.findViewById(R.id.btn_brush_state);
        recyclerView_color = (RecyclerView) itemView.findViewById(R.id.recycler_color);
        recyclerView_color.setHasFixedSize(true);
        recyclerView_color.setLayoutManager(new LinearLayoutManager(getActivity(),LinearLayoutManager.HORIZONTAL,false));

        colorAdapter = new ColorAdapter(getContext(),genColorList(), (ColorAdapter.ColorAdapterListener) this);
        recyclerView_color.setAdapter(colorAdapter);

        seekBar_brush_size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                listener.onBrushSizeChangedListener(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBar_brush_color.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                listener.onBrushColorChangedListener(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        btn_brush_state.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                listener.onBrushStateChangedListener(b);
            }
        });

        return itemView;
    }

    private List<Integer> genColorList(){
        List<Integer> colorList = new ArrayList<>();

        colorList.add(Color.parseColor("#131722"));
        colorList.add(Color.parseColor("#eb4b35"));
        colorList.add(Color.parseColor("#ebbe20"));
        colorList.add(Color.parseColor("#badeda"));
        colorList.add(Color.parseColor("#f8f1ea"));
        colorList.add(Color.parseColor("#79893c"));

        return colorList;
    }

    public void onColorSelected(int color){
        listener.onBrushColorChangedListener(color);
    }


}