import React, { useState, useEffect } from 'react';
import Header from './components/Header.jsx';
import UrlInputCard from './components/UrlInputCard.jsx';
import MediaPreviewCard from './components/MediaPreviewCard.jsx';
import ProgressCard from './components/ProgressCard.jsx';
import { motion, AnimatePresence } from 'framer-motion';
import { AlertCircle, CheckCircle2, Info, X } from 'lucide-react';

export default function App() {
  const [inspecting, setInspecting] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const [media, setMedia] = useState(null);
  const [currentTaskId, setCurrentTaskId] = useState(null);
  const [progressData, setProgressData] = useState(null);
  const [toast, setToast] = useState(null); // { type: 'error' | 'success' | 'info', message: '' }

  const showToast = (toastObj) => {
    if (typeof toastObj === 'string') {
      setToast({ type: 'info', message: toastObj });
    } else {
      setToast(toastObj);
    }
  };

  useEffect(() => {
    if (toast) {
      const duration = toast.type === 'error' ? 6000 : 3500;
      const timer = setTimeout(() => setToast(null), duration);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  // Inspect URL Handler
  const handleInspect = async (url) => {
    setInspecting(true);
    setMedia(null);
    setProgressData(null);
    setCurrentTaskId(null);

    try {
      const res = await fetch('/api/inspect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url }),
      });
      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.detail || 'Gagal memeriksa URL.');
      }
      setMedia(data.data);
      showToast({ type: 'success', message: 'Media siap diunduh! ⚡' });
    } catch (err) {
      showToast({ type: 'error', message: err.message });
    } finally {
      setInspecting(false);
    }
  };

  // Download Action Handler
  const handleStartDownload = async (options) => {
    setDownloading(true);
    setProgressData({
      status: 'pending',
      progress: 2,
      message: 'Menghubungkan ke engine downloader...',
    });

    try {
      const res = await fetch('/api/download', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(options),
      });
      const data = await res.json();
      if (!res.ok || !data.success) {
        throw new Error(data.detail || 'Gagal memulai unduhan.');
      }
      setCurrentTaskId(data.task_id);
    } catch (err) {
      setDownloading(false);
      setProgressData({
        status: 'failed',
        error: err.message,
        message: 'Gagal memulai unduhan.',
      });
      showToast({ type: 'error', message: err.message });
    }
  };

  // Server-Sent Events (SSE) Listener for Progress
  useEffect(() => {
    if (!currentTaskId) return;

    const eventSource = new EventSource(`/api/progress/${currentTaskId}`);

    eventSource.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data);
        setProgressData(payload);

        if (payload.status === 'completed') {
          setDownloading(false);
          eventSource.close();
          showToast({ type: 'success', message: 'Unduhan berhasil disimpan! 🎉' });
        } else if (payload.status === 'failed') {
          setDownloading(false);
          eventSource.close();
          showToast({ type: 'error', message: payload.error || 'Unduhan gagal diproses.' });
        }
      } catch (e) {
        console.error('Error parsing SSE event:', e);
      }
    };

    eventSource.onerror = (err) => {
      console.warn('SSE connection closed:', err);
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [currentTaskId]);

  const handleReset = () => {
    setMedia(null);
    setCurrentTaskId(null);
    setProgressData(null);
    setDownloading(false);
  };

  return (
    <div className="min-h-screen flex flex-col justify-between py-2 bg-[#F8FAFC] text-slate-900 selection:bg-[#FFE600] selection:text-slate-900">
      
      {/* Tactile Toast Notification (Fixed layout, never offside) */}
      <AnimatePresence>
        {toast && (
          <motion.div
            initial={{ opacity: 0, y: -25, scale: 0.94 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -20, scale: 0.94 }}
            transition={{ type: "spring", stiffness: 350, damping: 20 }}
            className={`fixed top-4 left-1/2 -translate-x-1/2 z-50 w-[calc(100vw-32px)] max-w-md p-4 rounded-2xl border-2 border-slate-900 shadow-[0_5px_0_0_#0F172A] flex items-start gap-3 ${
              toast.type === 'error'
                ? 'bg-[#FEF2F2] text-rose-950'
                : toast.type === 'success'
                ? 'bg-[#ECFDF5] text-emerald-950'
                : 'bg-white text-slate-900'
            }`}
          >
            <div className="flex-shrink-0 mt-0.5">
              {toast.type === 'error' ? (
                <div className="w-6 h-6 rounded-lg bg-rose-500 text-white flex items-center justify-center font-black">
                  <AlertCircle size={15} strokeWidth={2.5} />
                </div>
              ) : toast.type === 'success' ? (
                <div className="w-6 h-6 rounded-lg bg-emerald-500 text-white flex items-center justify-center font-black">
                  <CheckCircle2 size={15} strokeWidth={3} />
                </div>
              ) : (
                <div className="w-6 h-6 rounded-lg bg-indigo-500 text-white flex items-center justify-center font-black">
                  <Info size={15} strokeWidth={2.5} />
                </div>
              )}
            </div>

            <div className="flex-1 text-xs sm:text-sm font-bold break-words leading-snug">
              {toast.message}
            </div>

            <button
              onClick={() => setToast(null)}
              className="flex-shrink-0 p-1 text-slate-400 hover:text-slate-900 rounded-lg transition-colors"
            >
              <X size={15} />
            </button>
          </motion.div>
        )}
      </AnimatePresence>

      <div>
        {/* Navigation & Header */}
        <Header onToast={showToast} />

        {/* URL Input Form */}
        <UrlInputCard onInspect={handleInspect} loading={inspecting} />

        {/* Media Preview & Download Form */}
        {media && (
          <MediaPreviewCard
            media={media}
            onStartDownload={handleStartDownload}
            downloading={downloading}
          />
        )}

        {/* Progress Display */}
        {progressData && (
          <ProgressCard
            progressData={progressData}
            onReset={handleReset}
            onToast={showToast}
          />
        )}
      </div>

      {/* Clean Minimalist Playful Footer */}
      <footer className="w-full max-w-3xl mx-auto px-4 py-6 text-center text-xs font-bold text-slate-400">
        <p>Universal Downloader</p>
      </footer>

    </div>
  );
}
