import React, { useState } from 'react';
import { 
  Video, Music, Image as ImageIcon, FileText, Check, 
  Download, Clock, User, Layers, CheckSquare, Square
} from 'lucide-react';
import { motion } from 'framer-motion';

export default function MediaPreviewCard({ media, onStartDownload, downloading }) {
  const isPhoto = Boolean(media.is_photo);
  const isCarousel = Boolean(media.is_carousel && media.slides?.length > 0);

  const [activeTab, setActiveTab] = useState(isPhoto ? 'image' : 'video'); // 'video', 'audio', 'image', 'extras'
  const [selectedQuality, setSelectedQuality] = useState(
    media.video_resolutions?.[0] || '1080p'
  );
  const [selectedBitrate, setSelectedBitrate] = useState('320k');
  const [extraType, setExtraType] = useState('thumbnail');

  const [selectedSlideIndices, setSelectedSlideIndices] = useState(
    isCarousel ? media.slides.map((_, i) => i) : []
  );

  const toggleSlide = (idx) => {
    if (selectedSlideIndices.includes(idx)) {
      setSelectedSlideIndices(selectedSlideIndices.filter(i => i !== idx));
    } else {
      setSelectedSlideIndices([...selectedSlideIndices, idx].sort((a, b) => a - b));
    }
  };

  const toggleSelectAll = () => {
    if (selectedSlideIndices.length === media.slides.length) {
      setSelectedSlideIndices([]);
    } else {
      setSelectedSlideIndices(media.slides.map((_, i) => i));
    }
  };

  const handleDownloadClick = () => {
    let payload = {
      url: media.webpage_url,
      type: activeTab === 'extras' ? extraType : activeTab,
      quality: activeTab === 'video' ? selectedQuality : selectedBitrate,
      is_carousel: isCarousel,
      slide_indices: isCarousel ? selectedSlideIndices : null,
    };
    onStartDownload(payload);
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.94, y: 15 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      transition={{ type: "spring", stiffness: 320, damping: 22 }}
      className="w-full max-w-3xl mx-auto px-4 mb-6"
    >
      <div className="card-tactile p-6 sm:p-7">
        
        {/* Top Info Header */}
        <div className="flex flex-col sm:flex-row gap-5 items-start">
          
          {/* Main Thumbnail */}
          <div className="w-full sm:w-48 h-32 flex-shrink-0 bg-slate-100 rounded-2xl overflow-hidden border-2 border-slate-900 shadow-[0_3px_0_0_#0F172A] relative group">
            {media.thumbnail ? (
              <img
                src={media.thumbnail}
                alt={media.title}
                className="w-full h-full object-cover transition-transform duration-200 group-hover:scale-105"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-slate-400">
                <Video size={36} />
              </div>
            )}
            {media.duration && (
              <div className="absolute bottom-2 right-2 px-2 py-0.5 rounded-lg bg-black/85 text-white text-xs font-black flex items-center gap-1 border border-white/20">
                <Clock size={11} strokeWidth={2.5} />
                {media.duration}
              </div>
            )}
          </div>

          {/* Media Info */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1.5 flex-wrap">
              <span className="px-2.5 py-0.5 rounded-lg border-2 border-slate-900 bg-[#E0F2FE] text-sky-900 text-xs font-black tracking-wide shadow-[0_2px_0_0_#0F172A]">
                {media.extractor}
              </span>
              {isCarousel && (
                <span className="px-2.5 py-0.5 rounded-lg border-2 border-slate-900 bg-[#F3E8FF] text-purple-900 text-xs font-black flex items-center gap-1 shadow-[0_2px_0_0_#0F172A]">
                  <Layers size={13} strokeWidth={2.5} />
                  {media.slides.length} Slide Carousel
                </span>
              )}
            </div>

            <h2 className="text-base sm:text-lg font-black text-slate-900 line-clamp-2 leading-snug">
              {media.title}
            </h2>

            <div className="flex items-center gap-1.5 text-xs font-bold text-slate-600 mt-2">
              <User size={13} className="text-slate-400" />
              <span>{media.author}</span>
            </div>
          </div>

        </div>

        {/* Carousel Multi-Slide Selector */}
        {isCarousel && (
          <div className="mt-6 pt-5 border-t-2 border-slate-900/10">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <Layers size={16} className="text-purple-600" />
                <span className="text-xs sm:text-sm font-black text-slate-900">
                  Pilih Slide:
                </span>
                <span className="px-2 py-0.5 rounded-md bg-purple-100 text-purple-900 text-xs font-black">
                  {selectedSlideIndices.length} dari {media.slides.length}
                </span>
              </div>
              <button
                type="button"
                onClick={toggleSelectAll}
                className="btn-tactile text-xs font-black text-slate-900 flex items-center gap-1 py-1 px-2.5 rounded-xl bg-white"
              >
                {selectedSlideIndices.length === media.slides.length ? (
                  <>
                    <CheckSquare size={13} strokeWidth={2.5} />
                    <span>Batal Semua</span>
                  </>
                ) : (
                  <>
                    <Square size={13} strokeWidth={2.5} />
                    <span>Pilih Semua</span>
                  </>
                )}
              </button>
            </div>

            {/* Slide Thumbnails Grid with Playful Micro-Tilt */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 max-h-56 overflow-y-auto p-1.5">
              {media.slides.map((slide, idx) => {
                const isSelected = selectedSlideIndices.includes(idx);
                return (
                  <motion.div
                    key={slide.id || idx}
                    onClick={() => toggleSlide(idx)}
                    whileHover={{ scale: 1.04, rotate: (idx % 2 === 0 ? 1 : -1) }}
                    className={`cursor-pointer rounded-xl border-2 p-1.5 transition-all flex flex-col gap-1 relative ${
                      isSelected
                        ? 'border-slate-900 bg-[#FAF5FF] shadow-[0_3px_0_0_#0F172A]'
                        : 'border-slate-300 bg-white opacity-60 hover:opacity-100'
                    }`}
                  >
                    <div className="w-full h-20 bg-slate-100 rounded-lg overflow-hidden relative">
                      {slide.thumbnail ? (
                        <img src={slide.thumbnail} alt={slide.title} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-slate-300">
                          {slide.is_video ? <Video size={20} /> : <ImageIcon size={20} />}
                        </div>
                      )}
                      
                      <div className="absolute top-1 left-1 px-1.5 py-0.5 rounded-md bg-slate-900 text-[10px] font-black text-white">
                        #{idx + 1}
                      </div>

                      <div className={`absolute top-1 right-1 w-5 h-5 rounded-md flex items-center justify-center border-2 border-slate-900 text-xs font-black ${
                        isSelected ? 'bg-purple-600 text-white' : 'bg-white'
                      }`}>
                        {isSelected && <Check size={12} strokeWidth={3} />}
                      </div>
                    </div>
                  </motion.div>
                );
              })}
            </div>
          </div>
        )}

        {/* Format Selection Tabs */}
        <div className="mt-6 pt-5 border-t-2 border-slate-900/10">
          <div className="flex items-center gap-2 mb-4 flex-wrap">
            {!isPhoto && (
              <button
                onClick={() => setActiveTab('video')}
                className={`btn-tactile flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-black transition-all ${
                  activeTab === 'video'
                    ? 'btn-tactile-indigo'
                    : 'bg-white text-slate-900'
                }`}
              >
                <Video size={16} strokeWidth={2.5} />
                Video MP4
              </button>
            )}

            {!isPhoto && (
              <button
                onClick={() => setActiveTab('audio')}
                className={`btn-tactile flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-black transition-all ${
                  activeTab === 'audio'
                    ? 'btn-tactile-emerald'
                    : 'bg-white text-slate-900'
                }`}
              >
                <Music size={16} strokeWidth={2.5} />
                Audio MP3
              </button>
            )}

            {isPhoto && (
              <button
                onClick={() => setActiveTab('image')}
                className={`btn-tactile flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-black transition-all ${
                  activeTab === 'image'
                    ? 'btn-tactile-coral'
                    : 'bg-white text-slate-900'
                }`}
              >
                <ImageIcon size={16} strokeWidth={2.5} />
                Foto JPG
              </button>
            )}

            <button
              onClick={() => setActiveTab('extras')}
              className={`btn-tactile flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-black transition-all ${
                activeTab === 'extras'
                  ? 'bg-slate-900 text-white'
                  : 'bg-white text-slate-900'
              }`}
            >
              <FileText size={16} strokeWidth={2.5} />
              Ekstra
            </button>
          </div>

          {/* Quality Options per Tab */}
          <div className="bg-slate-50 p-4 rounded-2xl border-2 border-slate-900/15 mb-5">
            {activeTab === 'video' && (
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs font-black text-slate-600 mr-1 uppercase">Resolusi:</span>
                {(media.video_resolutions || ['1080p', '720p', '480p']).map((res) => (
                  <button
                    key={res}
                    type="button"
                    onClick={() => setSelectedQuality(res)}
                    className={`btn-tactile px-3.5 py-1.5 rounded-xl text-xs font-black transition-all ${
                      selectedQuality === res
                        ? 'btn-tactile-indigo'
                        : 'bg-white text-slate-900'
                    }`}
                  >
                    {res}
                  </button>
                ))}
              </div>
            )}

            {activeTab === 'audio' && (
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs font-black text-slate-600 mr-1 uppercase">Kualitas:</span>
                {[
                  { label: '320 kbps (HQ)', val: '320k' },
                  { label: '192 kbps (Standard)', val: '192k' },
                  { label: '128 kbps (Hemat)', val: '128k' },
                ].map((item) => (
                  <button
                    key={item.val}
                    type="button"
                    onClick={() => setSelectedBitrate(item.val)}
                    className={`btn-tactile px-3.5 py-1.5 rounded-xl text-xs font-black transition-all ${
                      selectedBitrate === item.val
                        ? 'btn-tactile-emerald'
                        : 'bg-white text-slate-900'
                    }`}
                  >
                    {item.label}
                  </button>
                ))}
              </div>
            )}

            {activeTab === 'image' && (
              <div className="text-xs font-bold text-slate-700">
                Menyimpan foto asli beresolusi penuh dalam format .jpg ke folder lokal.
              </div>
            )}

            {activeTab === 'extras' && (
              <div className="flex items-center gap-2 flex-wrap">
                <button
                  type="button"
                  onClick={() => setExtraType('thumbnail')}
                  className={`btn-tactile px-3.5 py-1.5 rounded-xl text-xs font-black ${
                    extraType === 'thumbnail'
                      ? 'bg-slate-900 text-white'
                      : 'bg-white text-slate-900'
                  }`}
                >
                  Thumbnail HD (.jpg)
                </button>
                <button
                  type="button"
                  onClick={() => setExtraType('subtitles')}
                  className={`btn-tactile px-3.5 py-1.5 rounded-xl text-xs font-black ${
                    extraType === 'subtitles'
                      ? 'bg-slate-900 text-white'
                      : 'bg-white text-slate-900'
                  }`}
                >
                  Subtitles (.srt)
                </button>
              </div>
            )}
          </div>

          {/* Big Tactile Gass Unduh Button */}
          <motion.button
            whileTap={{ scale: 0.97 }}
            onClick={handleDownloadClick}
            disabled={downloading || (isCarousel && selectedSlideIndices.length === 0)}
            className="btn-tactile btn-tactile-coral w-full py-4 rounded-2xl font-black text-base sm:text-lg flex items-center justify-center gap-2.5 cursor-pointer disabled:opacity-50 disabled:pointer-events-none"
          >
            <Download size={22} strokeWidth={3} />
            <span>
              {isCarousel
                ? `Gass Unduh (${selectedSlideIndices.length} Slide)`
                : activeTab === 'video'
                ? `Gass Unduh Video (${selectedQuality})`
                : activeTab === 'audio'
                ? `Gass Unduh MP3 (${selectedBitrate})`
                : activeTab === 'image'
                ? 'Gass Unduh Foto (JPG)'
                : 'Gass Unduh Media'}
            </span>
          </motion.button>

        </div>

      </div>
    </motion.div>
  );
}
