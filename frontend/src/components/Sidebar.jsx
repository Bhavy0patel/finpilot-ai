import React from 'react';
import { LayoutDashboard, FileSpreadsheet, AlertTriangle, Sparkles, History, Settings } from 'lucide-react';

export default function Sidebar({ activeTab, setActiveTab, exceptionCount = 0 }) {
  const navItems = [
    { id: 'dashboard', label: 'Overview', icon: LayoutDashboard },
    { id: 'upload', label: 'Reconcile Data', icon: FileSpreadsheet },
    { id: 'exceptions', label: 'Exceptions Feed', icon: AlertTriangle, badge: exceptionCount },
    { id: 'table', label: 'Records Table', icon: History },
  ];

  return (
    <aside className="sidebar">
      <div>
        <div style={{ padding: '0 12px 24px', display: 'flex', alignItems: 'center', gap: '10px' }}>
          <div style={{
            width: '28px',
            height: '28px',
            borderRadius: '8px',
            background: '#111827',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#D2FF00',
            fontWeight: 800,
            fontSize: '14px'
          }}>
            FP
          </div>
          <span style={{ fontWeight: 800, fontSize: '15px', letterSpacing: '-0.02em' }}>FinPilot</span>
        </div>

        <div className="nav-stack">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`nav-item ${isActive ? 'active' : ''}`}
              >
                <Icon size={18} />
                <span>{item.label}</span>
                {item.badge > 0 && (
                  <span className="sidebar-badge">{item.badge}</span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      <div style={{ borderTop: '1px solid #F3F4F6', paddingTop: '16px' }}>
        <div style={{ padding: '10px 14px', borderRadius: '16px', background: '#F9FAFB' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <Sparkles size={14} color="#10B981" />
            <span style={{ fontSize: '11px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em', color: '#374151' }}>
              Razorpay Buildathon
            </span>
          </div>
          <p style={{ fontSize: '11px', color: '#6B7280', lineHeight: 1.4 }}>
            Autonomous AI Finance Controller
          </p>
        </div>
      </div>
    </aside>
  );
}
