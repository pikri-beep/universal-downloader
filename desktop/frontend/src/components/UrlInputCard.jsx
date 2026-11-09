import React, { useState } from 'react';
import { Clipboard, ArrowRight, X, Loader2 } from 'lucide-react';
import { motion } from 'framer-motion';

export default function UrlInputCard({ onInspect, loading }) {
  const [url, setUrl] = useState('');

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        setUrl(text.trim());
      }
    } catch (err) {
      console.warn('Clipboard read failed:', err);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!url.trim() || loading) return;
    onInspect(url.trim());
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: "spring", stiffness: 300, damping: 22 }}
      className="w-full max-w-3xl mx-auto px-4 mb-6"
    >
      <div className="card-tactile p-6 sm:p-7">
        <form onSubmit={handleSubmit} className="flex flex-col gap-3.5">
          
          <div className="flex items-center justify-between">
            <label className="text-xs sm:text-sm font-black text-slate-900 tracking-wide flex items-center gap-1.5">
              <span>Masukkan Link Media</span>
              <span className="text-slate-400 font-semibold text-xs">(YouTube, TikTok, IG, X, dll)</span>
            </label>
          </div>

          <div className="relative flex items-center">
            <input
              type="text"
              value={url}
              onChange={(e) => setUrl(e.target.value)}
              placeholder="https://..."
              className="w-full px-4 py-3.5 pr-28 text-sm sm:text-base bg-slate-50 border-2 border-slate-900 rounded-2xl focus:outline-none focus:bg-white focus:ring-2 focus:ring-[#FFE600] transition-all text-slate-900 placeholder-slate-400 font-bold"
            />

            {/* Clear Button */}
            {url && (
              <button
                type="button"
                onClick={() => setUrl('')}
                className="absolute right-24 p-1.5 text-slate-400 hover:text-slate-900 rounded-lg transition-colors"
                title="Hapus"
              >
                <X size={17} />
              </button>
            )}

            {/* Tactile Paste Button */}
            <button
              type="button"
              onClick={handlePaste}
              className="absolute right-2.5 btn-tactile flex items-center gap-1 px-3 py-1.5 rounded-xl bg-[#FFE600] text-slate-900 text-xs font-black"
            >
              <Clipboard size={13} strokeWidth={2.5} />
              <span>Tempel</span>
            </button>
          </div>

          <div className="flex justify-end pt-1">
            <motion.button
              type="submit"
              disabled={!url.trim() || loading}
              whileTap={{ scale: 0.96 }}
              className="btn-tactile btn-tactile-coral w-full sm:w-auto px-6 py-3 rounded-2xl font-black text-sm sm:text-base flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50 disabled:pointer-events-none"
            >
              {loading ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  <span>Memeriksa Media...</span>
                </>
              ) : (
                <>
                  <span>Periksa & Siapkan</span>
                  <ArrowRight size={18} strokeWidth={2.5} />
                </>
              )}
            </motion.button>
          </div>

        </form>
      </div>
    </motion.div>
  );
}
