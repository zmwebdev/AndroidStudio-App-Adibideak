import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import eus.zubirimanteo.misiogalaktikoa.data.model.Misioa
import eus.zubirimanteo.misiogalaktikoa.databinding.MisioItemBinding // 👈 Item bakoitzaren layout-a

// ⚠️ Eredu honetan, misio_item.xml fitxategia behar da 

class MisioAdapter(private val clickListener: (Misioa) -> Unit) :
    ListAdapter<Misioa, MisioAdapter.MisioViewHolder>(MisioDiffCallback()) {

    // 1. ViewHolder (Item bakoitzaren bista gordetzen du)
    class MisioViewHolder(private val binding: MisioItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(misioa: Misioa, clickListener: (Misioa) -> Unit) {
            // Suponatzen dugu TextView-ak ditugula 'misioIzenaText' eta 'misioDataText' ID-ekin
            binding.misioIzenaText.text = misioa.izena
            binding.misioDataText.text = misioa.hasieraData.toString()

            // Item-ean klik egiteko funtzioa
            itemView.setOnClickListener { clickListener(misioa) }
        }
    }

    // 2. onCreateViewHolder: Item-a puztu eta ViewHolder sortu
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisioViewHolder {
        val binding = MisioItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MisioViewHolder(binding)
    }

    // 3. onBindViewHolder: Datuak lotu
    override fun onBindViewHolder(holder: MisioViewHolder, position: Int) {
        val misioa = getItem(position) // ListAdapter-en funtzioa
        holder.bind(misioa, clickListener)
    }

    // 4. DiffUtil.ItemCallback: Zerrenda modu eraginkorrean eguneratzeko logika
    class MisioDiffCallback : DiffUtil.ItemCallback<Misioa>() {
        override fun areItemsTheSame(oldItem: Misioa, newItem: Misioa): Boolean {
            // Bi elementu berdinak al dira (IDa)?
            return oldItem.misioaId == newItem.misioaId
        }

        override fun areContentsTheSame(oldItem: Misioa, newItem: Misioa): Boolean {
            // Bi elementuen edukiak berdinak al dira?
            return oldItem == newItem
        }
    }
}