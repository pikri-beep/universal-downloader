import React, { useEffect } from 'react';
import { CheckCircle2, AlertCircle, Folder, ArrowUpRight, Loader2 } from 'lucide-react';
import { motion } from 'framer-motion';
import confetti from 'canvas-confetti';

export default function ProgressCard({ progressData, onReset, onToast }) {
  if (!progressData) return null;

  const {
    status,
    progress = 0,
    speed_str = '',
    eta_str = '',
    message = '',
    file_name = '',
    file_path = '',
    error = '',
  } = progressData;

  const isCompleted = status === 'completed';
  const isFailed = status === 'failed';
  const isProcessing = !isCompleted && !isFailed;

  // Trigger celebration confetti upon completion
  useEffect(() => {
    if (isCompleted) {
      try {
        confetti({
          particleCount: 50,
          spread: 65,
          origin: { y: 0.65 },
          colors: ['#FF5A5F', '#FFE600', '#6366F1', '#10B981'],
        });
      } catch (e) {}
    }
  }, [isCompleted]);

  const handleOpenFile = async () => {
    try {
      const res = await fetch('/api/open-file', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ file_path }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.detail || 'Gagal membuka file.');
      onToast({ type: 'info', message: 'Membuka file media...' });
    } catch (e) {
      onToast({ type: 'error', message: e.message || 'Gagal membuka file.' });
    }
  };

  const handleOpenFolder = async () => {
    try {
      await fetch('/api/open-folder', { method: 'POST' });
      onToast({ type: 'info', message: 'Membuka folder Downloads/UniversalDownloader' });
    } catch (e) {
      onToast({ type: 'error', message: 'Gagal membuka folder.' });
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 15 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ type: "spring", stiffness: 300, damping: 22 }}
      className="w-full max-w-3xl mx-auto px-4 mb-8"
    >
      <div className={`card-tactile p-6 sm:p-7 ${
        isCompleted
          ? 'bg-[#F0FDF4]'
          : isFailed
          ? 'bg-[#FEF2F2]'
          : 'bg-white'
      }`}>

        {/* Status Header */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            {isCompleted ? (
              <motion.div
                initial={{ scale: 0, rotate: -20 }}
                animate={{ scale: [0, 1.3, 1], rotate: [-20, 8, 0] }}
                transition={{ type: "spring", stiffness: 400, damping: 15 }}
                className="w-10 h-10 rounded-2xl bg-[#10B981] border-2 border-slate-900 shadow-[0_3px_0_0_#0F172A] text-white flex items-center justify-center font-black"
              >
                <CheckCircle2 size={22} strokeWidth={3} />
              </motion.div>
            ) : isFailed ? (
              <div className="w-10 h-10 rounded-2xl bg-[#EF4444] border-2 border-slate-900 shadow-[0_3px_0_0_#0F172A] text-white flex items-center justify-center">
                <AlertCircle size={22} strokeWidth={2.5} />
              </div>
            ) : (
              <div className="w-10 h-10 rounded-2xl bg-[#FFE600] border-2 border-slate-900 shadow-[0_3px_0_0_#0F172A] text-slate-900 flex items-center justify-center">
                <Loader2 size={20} strokeWidth={2.5} className="animate-spin" />
              </div>
            )}

            <div>
              <h3 className="text-base sm:text-lg font-black text-slate-900">
                {isCompleted
                  ? 'Beres! File Berhasil Diunduh 🎉'
                  : isFailed
                  ? 'Gagal Memproses Unduhan'
                  : 'Sedang Mengunduh Stream...'}
              </h3>
              <p className="text-xs sm:text-sm font-bold text-slate-500">
                {message || (isCompleted ? 'File sudah tersimpan di folder lokal.' : 'Menyiapkan...')}
              </p>
            </div>
          </div>

          {/* Progress Percentage Badge */}
          {isProcessing && (
            <motion.div
              key={Math.round(progress)}
              initial={{ scale: 1.15 }}
              animate={{ scale: 1 }}
              className="px-3 py-1 rounded-xl border-2 border-slate-900 bg-[#FFE600] text-slate-900 font-black text-sm shadow-[0_2px_0_0_#0F172A]"
            >
              {Math.round(progress)}%
            </motion.div>
          )}
        </div>

        {/* Animated Candy-Stripe Progress Bar */}
        {isProcessing && (
          <div className="mb-4">
            <div className="w-full h-5 bg-slate-100 rounded-2xl overflow-hidden border-2 border-slate-900 shadow-[inset_0_2px_4px_rgba(0,0,0,0.1)] p-0.5">
              <motion.div
                initial={{ width: 0 }}
                animate={{ width: `${Math.min(100, Math.max(3, progress))}%` }}
                transition={{ ease: "easeOut", duration: 0.2 }}
                className="h-full bg-[#FF5A5F] rounded-xl candy-striped transition-all"
              />
            </div>

            <div className="flex items-center justify-between text-xs font-black text-slate-500 mt-2 px-1">
              <span>Kecepatan: {speed_str || '-'}</span>
              <span>Sisa Waktu: {eta_str || '-'}</span>
            </div>
          </div>
        )}

        {/* Completed State Card */}
        {isCompleted && (
          <div className="bg-white p-4 rounded-2xl border-2 border-slate-900 shadow-[0_3px_0_0_#0F172A] mb-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3.5">
            <div className="min-w-0">
              <span className="text-[10px] font-black text-emerald-700 uppercase tracking-wider block">
                File Tersimpan:
              </span>
              <p className="text-xs sm:text-sm font-black text-slate-900 truncate max-w-lg mt-0.5">
                {file_name || 'Media File'}
              </p>
            </div>

            <div className="flex items-center gap-2 flex-shrink-0 w-full sm:w-auto">
              {file_path && (
                <button
                  type="button"
                  onClick={handleOpenFile}
                  className="btn-tactile btn-tactile-emerald flex-1 sm:flex-none flex items-center justify-center gap-1.5 px-4 py-2 rounded-xl text-xs font-black cursor-pointer"
                >
                  <ArrowUpRight size={15} strokeWidth={2.5} />
                  Buka File
                </button>
              )}
              <button
                type="button"
                onClick={handleOpenFolder}
                className="btn-tactile flex items-center justify-center gap-1.5 px-3.5 py-2 rounded-xl bg-white text-slate-900 text-xs font-black"
              >
                <Folder size={14} className="text-amber-500" />
                Folder
              </button>
            </div>
          </div>
        )}

        {/* Error Message */}
        {isFailed && (
          <div className="bg-white p-4 rounded-xl border-2 border-rose-800 text-rose-900 text-xs font-bold mb-4 break-words shadow-[0_2px_0_0_#991B1B]">
            {error || 'Terjadi gangguan saat mengambil stream. Coba perbarui engine atau periksa link.'}
          </div>
        )}

        {/* Reset Action */}
        {(isCompleted || isFailed) && (
          <button
            type="button"
            onClick={onReset}
            className="btn-tactile text-xs font-black text-slate-900 px-4 py-2 rounded-xl bg-white hover:bg-slate-50 transition-colors"
          >
            ← Unduh Media Lain
          </button>
        )}

      </div>
    </motion.div>
  );
}
