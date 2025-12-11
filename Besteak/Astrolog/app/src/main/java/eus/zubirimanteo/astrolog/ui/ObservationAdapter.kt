package eus.zubirimanteo.astrolog.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eus.zubirimanteo.astrolog.data.Observation
import eus.zubirimanteo.astrolog.databinding.ListItemObservationBinding

/**
 * [ObservationAdapter] RecyclerView-erako. Behaketen zerrenda erakutsi eta
 * elementuak klikatzean ekintzak kudeatzen ditu.
 * ListAdapter erabiltzen dugu errendimendua hobetzeko DiffUtil-ekin.
 */
class ObservationAdapter(private val clickListener: (Observation) -> Unit) :
    ListAdapter<Observation, ObservationAdapter.ObservationViewHolder>(DiffCallback) {

    // ViewBinding erabiltzen dugu ViewHolder-ean
    class ObservationViewHolder(private var binding: ListItemObservationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(observation: Observation) {
            // Room-eko datuak UI elementuetan kargatu
            binding.celestialBodyText.text = observation.celestialBody
            binding.dateText.text = observation.date

            // Oharren aurrebista sortu (lehen 100 karaktereak)
            val notesPreview = if (observation.notes.length > 100) {
                observation.notes.substring(0, 100) + "..."
            } else {
                observation.notes
            }
            binding.notesPreviewText.text = notesPreview
            binding.scopeChip.text = observation.scopeSetting
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObservationViewHolder {
        val binding = ListItemObservationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        // ViewHolder sortzean, klik-ekintza definitu
        return ObservationViewHolder(binding).also { viewHolder ->
            binding.root.setOnClickListener {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val observation = getItem(position)
                    clickListener(observation)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ObservationViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    // DiffUtil.ItemCallback ListAdapter-ek behar duen eraginkortasuna lortzeko
    companion object DiffCallback : DiffUtil.ItemCallback<Observation>() {
        override fun areItemsTheSame(oldItem: Observation, newItem: Observation): Boolean {
            // Itemak berdinak dira Room ID-a bera bada
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Observation, newItem: Observation): Boolean {
            // Edukiak berdinak dira datuak berdinak badira (data class-aren equals erabiliz)
            return oldItem == newItem
        }
    }
}