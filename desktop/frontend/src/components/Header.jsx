import React, { useState } from 'react';
import { Folder, RefreshCw, Power } from 'lucide-react';
import { motion } from 'framer-motion';

export default function Header({ onToast }) {
  const [updating, setUpdating] = useState(false);
  const [shuttingDown, setShuttingDown] = useState(false);

  const handleOpenFolder = async () => {
    try {
      await fetch('/api/open-folder', { method: 'POST' });
      onToast({ type: 'info', message: 'Membuka folder Downloads/UniversalDownloader' });
    } catch (e) {
      onToast({ type: 'error', message: 'Gagal membuka folder.' });
    }
  };

  const handleUpdateEngine = async () => {
    setUpdating(true);
    onToast({ type: 'info', message: 'Sedang mengecek dan memperbarui engine...' });
    try {
      const res = await fetch('/api/update-engine', { method: 'POST' });
      const data = await res.json();
      if (data.success) {
        onToast({ type: 'success', message: 'Engine berhasil di-update ke versi terbaru! 🚀' });
      } else {
        onToast({ type: 'info', message: 'Engine sudah versi paling mutakhir.' });
      }
    } catch (e) {
      onToast({ type: 'error', message: 'Gagal update engine. Cek koneksi internet.' });
    } finally {
      setUpdating(false);
    }
  };

  const handleShutdown = async () => {
    if (!window.confirm('Matikan server aplikasi?')) return;
    setShuttingDown(true);
    try {
      await fetch('/api/shutdown', { method: 'POST' });
      onToast({ type: 'info', message: 'Server dimatikan. Tab dapat ditutup.' });
      setTimeout(() => {
        window.close();
      }, 700);
    } catch (e) {
      onToast({ type: 'info', message: 'Server dimatikan.' });
    }
  };

  return (
    <header className="w-full max-w-3xl mx-auto mb-6 pt-6 px-4">
      <div className="flex items-center justify-between gap-4 pb-4 border-b-2 border-slate-900/10">
        
        {/* Brand */}
        <div className="flex items-center gap-3">
          <motion.div
            whileHover={{ rotate: [-6, 6, -3, 3, 0], scale: 1.08 }}
            transition={{ duration: 0.35 }}
            className="w-11 h-11 rounded-2xl bg-[#FFE600] border-2 border-slate-900 shadow-[0_3px_0_0_#0F172A] flex items-center justify-center text-xl cursor-pointer select-none"
          >
            ⚡
          </motion.div>
          <div>
            <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
              Universal Downloader
            </h1>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2 sm:gap-2.5">
          {/* Status Badge */}
          <div className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-xl border-2 border-slate-900 bg-[#E8F5E9] text-emerald-800 text-xs font-bold shadow-[0_2px_0_0_#0F172A]">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-ping"></span>
            Siap Gas
          </div>

          {/* Folder Button */}
          <button
            onClick={handleOpenFolder}
            className="btn-tactile flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white text-slate-900 text-xs font-bold"
            title="Buka Folder Unduhan"
          >
            <Folder size={14} className="text-amber-500" />
            <span className="hidden sm:inline">Folder</span>
          </button>

          {/* Update Engine Button */}
          <button
            onClick={handleUpdateEngine}
            disabled={updating}
            className="btn-tactile flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white text-slate-900 text-xs font-bold disabled:opacity-50"
            title="Update Engine yt-dlp"
          >
            <RefreshCw size={14} className={`text-indigo-600 ${updating ? 'animate-spin' : ''}`} />
            <span className="hidden sm:inline">{updating ? 'Update...' : 'Update'}</span>
          </button>

          {/* Shutdown Button */}
          <button
            onClick={handleShutdown}
            disabled={shuttingDown}
            className="btn-tactile flex items-center p-2 rounded-xl bg-[#FEE2E2] hover:bg-[#FCA5A5] text-rose-700 text-xs font-bold"
            title="Matikan Server"
          >
            <Power size={14} />
          </button>
        </div>

      </div>
    </header>
  );
}
