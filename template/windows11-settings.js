import React, { useState, useEffect } from 'react';
import {
    Home,
    Laptop,
    Bluetooth,
    Globe,
    Paintbrush,
    LayoutGrid,
    User,
    Clock,
    Gamepad2,
    Accessibility,
    ShieldCheck,
    RefreshCw,
    Search,
    ChevronRight,
    ChevronDown,
    Check,
    Minus,
    Square,
    X,
    Sparkles,
    Sliders,
    Eye,
    Monitor,
    Smile,
    Maximize2,
    Info
} from 'lucide-react';

// Theme Presets representing the exact 6 theme cards shown in Windows 11
const THEMES = [
    {
        id: 'win11-dark',
        name: 'Windows Dark (Default)',
        wallpaper: 'https://4kwallpapers.com/images/wallpapers/windows-11-dark-mode-blue-stock-official-3840x2160-5630.jpg',
        accent: '#0078d4', // Windows Blue
        accentTailwind: 'bg-blue-500',
        mode: 'dark',
        thumbnail: 'https://4kwallpapers.com/images/wallpapers/windows-11-dark-mode-blue-stock-official-3840x2160-5630.jpg'
    },
    {
        id: 'win11-light',
        name: 'Windows Light',
        wallpaper: 'https://images.unsplash.com/photo-1620121692029-d088224ddc74?q=80&w=2560&auto=format&fit=crop',
        accent: '#005a9e',
        accentTailwind: 'bg-sky-500',
        mode: 'light',
        thumbnail: 'https://images.unsplash.com/photo-1620121692029-d088224ddc74?q=80&w=300&auto=format&fit=crop'
    },
    {
        id: 'glow',
        name: 'Glow (Purple Sunset)',
        wallpaper: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=2564&auto=format&fit=crop',
        accent: '#b4009e', // Pink-Purple
        accentTailwind: 'bg-fuchsia-500',
        mode: 'dark',
        thumbnail: 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=300&auto=format&fit=crop'
    },
    {
        id: 'sunrise',
        name: 'Sunrise (Warm Skies)',
        wallpaper: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=2560&auto=format&fit=crop',
        accent: '#e05c00', // Orange Accent
        accentTailwind: 'bg-orange-500',
        mode: 'light',
        thumbnail: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?q=80&w=300&auto=format&fit=crop'
    },
    {
        id: 'captured-motion',
        name: 'Captured Motion',
        wallpaper: 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?q=80&w=2560&auto=format&fit=crop',
        accent: '#ff4b4b',
        accentTailwind: 'bg-rose-500',
        mode: 'dark',
        thumbnail: 'https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?q=80&w=300&auto=format&fit=crop'
    },
    {
        id: 'flow',
        name: 'Flow / Glacier Mint',
        wallpaper: 'https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=2560&auto=format&fit=crop',
        accent: '#10b981', // Emerald
        accentTailwind: 'bg-emerald-500',
        mode: 'dark',
        thumbnail: 'https://images.unsplash.com/photo-1550684848-fac1c5b4e853?q=80&w=300&auto=format&fit=crop'
    }
];

export default function App() {
    const [currentTheme, setCurrentTheme] = useState(THEMES[0]);
    const [activeTab, setActiveTab] = useState('Personalization');
    const [searchQuery, setSearchQuery] = useState('');
    const [transparencyEffects, setTransparencyEffects] = useState(true);
    const [isOpen, setIsOpen] = useState(true);
    const [isMaximized, setIsMaximized] = useState(false);
    const [time, setTime] = useState('');
    const [date, setDate] = useState('');
    const [customAccent, setCustomAccent] = useState('');

    // Detail views for settings items
    const [expandedSection, setExpandedSection] = useState(null);

    // System time updates
    useEffect(() => {
        const updateTime = () => {
            const now = new Date();
            setTime(now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
            setDate(now.toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' }));
        };
        updateTime();
        const interval = setInterval(updateTime, 1000);
        return () => clearInterval(interval);
    }, []);

    const handleThemeChange = (theme) => {
        setCurrentTheme(theme);
        if (theme.accent) {
            setCustomAccent(theme.accent);
        }
    };

    const toggleSection = (sectionName) => {
        if (expandedSection === sectionName) {
            setExpandedSection(null);
        } else {
            setExpandedSection(sectionName);
        }
    };

    // Filter items in Personalization tab
    const personalizationItems = [
        {
            id: 'background',
            title: 'Background',
            desc: 'Background image, color, slideshow',
            icon: ImageSectionIcon,
            details: (
                <div className="p-4 mt-2 rounded-lg bg-black/10 dark:bg-white/5 space-y-3 border border-white/5 text-sm">
                    <p className="font-semibold text-xs text-neutral-400 uppercase tracking-wider">Personalize your background</p>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                        {THEMES.map((t) => (
                            <button
                                key={t.id}
                                onClick={() => handleThemeChange(t)}
                                className={`p-1.5 rounded-md border text-xs flex flex-col items-center gap-1 transition ${currentTheme.id === t.id
                                        ? 'border-blue-500 bg-blue-500/10'
                                        : 'border-white/10 hover:bg-white/5'
                                    }`}
                            >
                                <img src={t.thumbnail} alt={t.name} className="w-full h-12 object-cover rounded" />
                                <span className="truncate w-full text-center text-[11px] font-medium">{t.name.split(' ')[0]}</span>
                            </button>
                        ))}
                    </div>
                    <div className="pt-2 border-t border-white/10 flex justify-between items-center text-xs">
                        <span>Choose a custom wallpaper image URL</span>
                        <input
                            type="text"
                            placeholder="Paste direct image link..."
                            className="bg-black/20 dark:bg-white/10 px-2 py-1 rounded border border-white/10 w-1/2 focus:outline-none focus:border-blue-500 text-xs"
                            onChange={(e) => {
                                if (e.target.value.trim() !== '') {
                                    setCurrentTheme(prev => ({ ...prev, wallpaper: e.target.value }));
                                }
                            }}
                        />
                    </div>
                </div>
            )
        },
        {
            id: 'colors',
            title: 'Colors',
            desc: 'Accent color, transparency effects, color theme',
            icon: Paintbrush,
            details: (
                <div className="p-4 mt-2 rounded-lg bg-black/10 dark:bg-white/5 space-y-4 border border-white/5 text-sm">
                    <div className="flex justify-between items-center">
                        <div>
                            <p className="font-medium">Transparency effects</p>
                            <p className="text-xs text-neutral-400">Windows and surfaces appear translucent (Mica/Acrylic)</p>
                        </div>
                        <button
                            onClick={() => setTransparencyEffects(!transparencyEffects)}
                            className={`w-11 h-6 rounded-full p-1 transition-colors duration-200 focus:outline-none ${transparencyEffects ? 'bg-sky-500' : 'bg-neutral-600'
                                }`}
                        >
                            <div className={`bg-white w-4 h-4 rounded-full shadow-md transform duration-200 ${transparencyEffects ? 'translate-x-5' : 'translate-x-0'
                                }`} />
                        </button>
                    </div>

                    <div className="border-t border-white/10 pt-3">
                        <p className="font-medium mb-2">Accent Color</p>
                        <div className="flex flex-wrap gap-2">
                            {['#0078d4', '#b4009e', '#e05c00', '#ff4b4b', '#10b981', '#6366f1', '#eab308'].map((color) => (
                                <button
                                    key={color}
                                    onClick={() => {
                                        setCustomAccent(color);
                                        setCurrentTheme(prev => ({ ...prev, accent: color }));
                                    }}
                                    className="w-8 h-8 rounded-full border border-white/20 flex items-center justify-center transition hover:scale-110 active:scale-95"
                                    style={{ backgroundColor: color }}
                                >
                                    {(currentTheme.accent === color || customAccent === color) && (
                                        <Check className="w-4 h-4 text-white drop-shadow-md" />
                                    )}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            )
        },
        {
            id: 'themes',
            title: 'Themes',
            desc: 'Install, create, manage',
            icon: LayoutGrid,
            details: (
                <div className="p-4 mt-2 rounded-lg bg-black/10 dark:bg-white/5 space-y-3 border border-white/5 text-sm">
                    <p className="font-medium">Current Theme Details</p>
                    <div className="grid grid-cols-2 gap-4 text-xs">
                        <div className="p-3 bg-white/5 rounded">
                            <span className="text-neutral-400 block">Name</span>
                            <span className="font-semibold">{currentTheme.name}</span>
                        </div>
                        <div className="p-3 bg-white/5 rounded">
                            <span className="text-neutral-400 block">Color Mode</span>
                            <span className="font-semibold capitalize">{currentTheme.mode} Mode</span>
                        </div>
                        <div className="p-3 bg-white/5 rounded">
                            <span className="text-neutral-400 block">Accent Color</span>
                            <span className="font-semibold font-mono flex items-center gap-1.5">
                                <span className="w-3 h-3 rounded-full inline-block" style={{ backgroundColor: currentTheme.accent }} />
                                {currentTheme.accent}
                            </span>
                        </div>
                        <div className="p-3 bg-white/5 rounded">
                            <span className="text-neutral-400 block">Acrylic Blurring</span>
                            <span className="font-semibold">{transparencyEffects ? "Enabled (30px)" : "Disabled"}</span>
                        </div>
                    </div>
                </div>
            )
        },
        {
            id: 'dynamic-lighting',
            title: 'Dynamic Lighting',
            desc: 'Connected devices, effects, app settings',
            icon: Sparkles,
            details: (
                <div className="p-4 mt-2 rounded-lg bg-black/10 dark:bg-white/5 text-xs text-neutral-400 italic">
                    No external dynamic lighting hardware (RGB keyboard/mouse) detected. Use a compatible device to activate.
                </div>
            )
        },
        {
            id: 'lockscreen',
            title: 'Lock screen',
            desc: 'Lock screen images, apps, animations',
            icon: Monitor,
            details: (
                <div className="p-4 mt-2 rounded-lg bg-black/10 dark:bg-white/5 text-sm space-y-2">
                    <p className="font-medium text-xs text-neutral-400">Lock screen preview</p>
                    <div className="relative w-full h-32 rounded overflow-hidden">
                        <img src={currentTheme.wallpaper} alt="Lock screen preview" className="w-full h-full object-cover" />
                        <div className="absolute inset-0 bg-black/30 flex flex-col items-center justify-center text-white">
                            <span className="text-2xl font-bold">{time}</span>
                            <span className="text-xs">{date}</span>
                        </div>
                    </div>
                </div>
            )
        },
        {
            id: 'textinput',
            title: 'Text input',
            desc: 'Touch keyboard, voice typing, emoji and more, input method editor',
            icon: Smile,
            details: (
                <div className="p-4 mt-2 rounded-lg bg-black/10 dark:bg-white/5 text-xs text-neutral-400">
                    Modify layouts, key sounds, speech recognition parameters, and keyboard themes.
                </div>
            )
        },
        {
            id: 'start',
            title: 'Start',
            desc: 'Recent apps and items, folders',
            icon: Home,
            details: (
                <div className="p-4 mt-2 rounded-lg bg-black/10 dark:bg-white/5 text-sm space-y-2">
                    <p className="font-medium">Start layout layout preferences</p>
                    <div className="flex gap-2">
                        <button className="flex-1 p-2 bg-white/5 rounded border border-white/10 hover:bg-white/10 text-xs">More pins</button>
                        <button className="flex-1 p-2 bg-white/10 rounded border border-white/20 text-xs font-semibold">Default</button>
                        <button className="flex-1 p-2 bg-white/5 rounded border border-white/10 hover:bg-white/10 text-xs">More recommendations</button>
                    </div>
                </div>
            )
        },
        {
            id: 'taskbar',
            title: 'Taskbar',
            desc: 'Taskbar behaviors, system pins',
            icon: Sliders,
            details: (
                <div className="p-4 mt-2 rounded-lg bg-black/10 dark:bg-white/5 text-sm space-y-3">
                    <p className="font-medium text-xs text-neutral-400">Taskbar alignment & features</p>
                    <div className="flex justify-between items-center">
                        <span>Taskbar alignment</span>
                        <select className="bg-black/30 dark:bg-white/10 px-2 py-1 rounded border border-white/10 text-xs focus:outline-none">
                            <option>Center (Standard)</option>
                            <option>Left</option>
                        </select>
                    </div>
                    <div className="flex justify-between items-center text-xs pt-2 border-t border-white/10 text-neutral-400">
                        <span>Show taskbar simulator on bottom of desktop?</span>
                        <span className="text-green-500 font-semibold">Active</span>
                    </div>
                </div>
            )
        }
    ];

    // Filter based on search query
    const filteredPersonalizationItems = personalizationItems.filter(item =>
        item.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.desc.toLowerCase().includes(searchQuery.toLowerCase())
    );

    const sidebarItems = [
        { name: 'Home', icon: Home },
        { name: 'System', icon: Laptop },
        { name: 'Bluetooth & devices', icon: Bluetooth },
        { name: 'Network & internet', icon: Globe },
        { name: 'Personalization', icon: Paintbrush },
        { name: 'Apps', icon: LayoutGrid },
        { name: 'Accounts', icon: User },
        { name: 'Time & language', icon: Clock },
        { name: 'Gaming', icon: Gamepad2 },
        { name: 'Accessibility', icon: Accessibility },
        { name: 'Privacy & security', icon: ShieldCheck },
        { name: 'Windows Update', icon: RefreshCw },
    ];

    const currentAccentColor = currentTheme.accent || '#0078d4';

    return (
        <div
            className="w-full h-screen relative flex flex-col justify-between overflow-hidden select-none font-sans transition-all duration-700"
            style={{
                backgroundImage: `url(${currentTheme.wallpaper})`,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                color: currentTheme.mode === 'dark' ? '#ffffff' : '#1c1c1c'
            }}
        >
            {/* Subtle overlay for light mode wallpaper visibility contrast */}
            <div className={`absolute inset-0 pointer-events-none transition-colors duration-500 ${currentTheme.mode === 'light' ? 'bg-white/10' : 'bg-black/20'
                }`} />

            {/* Main Container / Desktop Screen Area */}
            <div className="flex-1 w-full h-full flex items-center justify-center p-2 sm:p-4 z-10 overflow-hidden">

                {/* Settings Window */}
                {isOpen ? (
                    <div
                        className={`flex flex-col rounded-xl overflow-hidden shadow-[0_20px_50px_rgba(0,0,0,0.4)] border transition-all duration-300 ease-out ${isMaximized ? 'w-full h-full rounded-none' : 'w-full max-w-5xl h-[85vh] min-h-[500px]'
                            } ${currentTheme.mode === 'dark'
                                ? 'bg-[#1b1c20]/80 text-[#f5f5f5] border-white/10'
                                : 'bg-[#f3f3f3]/90 text-[#1c1c1c] border-black/10'
                            } ${transparencyEffects ? 'backdrop-blur-[35px]' : ''
                            }`}
                        style={{
                            boxShadow: '0 8px 32px 0 rgba(0, 0, 0, 0.37)'
                        }}
                    >
                        {/* Title Bar (Controls & Search Center) */}
                        <div className="flex items-center justify-between px-4 py-2.5 bg-black/10 dark:bg-white/5 border-b border-white/5">
                            <div className="flex items-center gap-2">
                                {/* Simulated Windows Icon in settings header */}
                                <div className="w-4 h-4 flex flex-wrap gap-0.5">
                                    <div className="w-[7px] h-[7px]" style={{ backgroundColor: currentAccentColor }}></div>
                                    <div className="w-[7px] h-[7px]" style={{ backgroundColor: currentAccentColor }}></div>
                                    <div className="w-[7px] h-[7px]" style={{ backgroundColor: currentAccentColor }}></div>
                                    <div className="w-[7px] h-[7px]" style={{ backgroundColor: currentAccentColor }}></div>
                                </div>
                                <span className="text-xs font-semibold select-none opacity-80">Settings</span>
                            </div>

                            {/* Windows search bar exactly replicating image layout */}
                            <div className="relative w-72 max-w-xs">
                                <span className="absolute inset-y-0 left-3 flex items-center pointer-events-none">
                                    <Search className="h-3.5 w-3.5 opacity-60" />
                                </span>
                                <input
                                    type="text"
                                    placeholder="Find a setting"
                                    value={searchQuery}
                                    onChange={(e) => setSearchQuery(e.target.value)}
                                    className={`w-full text-xs pl-9 pr-3 py-1.5 rounded-md border focus:outline-none transition ${currentTheme.mode === 'dark'
                                            ? 'bg-[#2d2d30]/70 border-white/10 focus:border-white/30 text-white placeholder-neutral-400'
                                            : 'bg-white/70 border-neutral-300 focus:border-neutral-400 text-neutral-800 placeholder-neutral-500'
                                        }`}
                                />
                            </div>

                            {/* Classic Windows Window Controls */}
                            <div className="flex items-center gap-1.5">
                                <button
                                    onClick={() => setIsOpen(false)}
                                    className="p-1.5 hover:bg-white/10 rounded transition-colors"
                                    title="Minimize"
                                >
                                    <Minus className="w-3.5 h-3.5" />
                                </button>
                                <button
                                    onClick={() => setIsMaximized(!isMaximized)}
                                    className="p-1.5 hover:bg-white/10 rounded transition-colors"
                                    title={isMaximized ? "Restore Down" : "Maximize"}
                                >
                                    <Square className="w-3 h-3" />
                                </button>
                                <button
                                    onClick={() => setIsOpen(false)}
                                    className="p-1.5 hover:bg-red-600 hover:text-white rounded transition-colors"
                                    title="Close"
                                >
                                    <X className="w-3.5 h-3.5" />
                                </button>
                            </div>
                        </div>

                        {/* Split Screen Application Body */}
                        <div className="flex-1 flex overflow-hidden">

                            {/* Left Sidebar (Navigation & Profile) */}
                            <div className={`w-64 flex-shrink-0 flex flex-col justify-between py-4 border-r overflow-y-auto ${currentTheme.mode === 'dark' ? 'border-white/5 bg-black/10' : 'border-neutral-200 bg-black/5'
                                }`}>
                                {/* User Profile Area (Top Left, matched exactly to the image) */}
                                <div className="px-4 mb-4 flex items-center gap-3">
                                    <div className="relative w-12 h-12 rounded-full overflow-hidden border border-white/20 flex-shrink-0">
                                        <img
                                            src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=256&auto=format&fit=crop"
                                            alt="RD28 Severino Avatar"
                                            className="w-full h-full object-cover"
                                        />
                                        <span className="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-[#1b1c20]" />
                                    </div>
                                    <div className="overflow-hidden">
                                        <h3 className="text-xs font-semibold truncate">RD28 Severino</h3>
                                        <p className="text-[10px] opacity-60 truncate">rd28@hotmail.es</p>
                                    </div>
                                </div>

                                {/* Sidebar Navigation */}
                                <nav className="flex-1 space-y-1 px-2">
                                    {sidebarItems.map((item) => {
                                        const isSelected = activeTab === item.name;
                                        return (
                                            <button
                                                key={item.name}
                                                onClick={() => setActiveTab(item.name)}
                                                className={`w-full flex items-center justify-between text-left px-3 py-2 rounded-md text-xs font-normal transition relative ${isSelected
                                                        ? currentTheme.mode === 'dark'
                                                            ? 'bg-white/5 text-white font-medium'
                                                            : 'bg-black/10 text-neutral-900 font-semibold'
                                                        : 'opacity-80 hover:bg-white/5 hover:opacity-100'
                                                    }`}
                                            >
                                                <div className="flex items-center gap-3">
                                                    {/* Accent bar on the left for active items, standard in Win 11 settings */}
                                                    {isSelected && (
                                                        <span
                                                            className="absolute left-0 top-2 bottom-2 w-[3px] rounded-r"
                                                            style={{ backgroundColor: currentAccentColor }}
                                                        />
                                                    )}
                                                    <item.icon className="w-4 h-4" style={{ color: isSelected ? currentAccentColor : 'inherit' }} />
                                                    <span className="truncate">{item.name}</span>
                                                </div>
                                            </button>
                                        );
                                    })}
                                </nav>

                                <div className="px-4 pt-4 border-t border-white/5 flex items-center gap-2 opacity-50 text-[10px]">
                                    <Info className="w-3.5 h-3.5" />
                                    <span>Win 11 Settings v23H2</span>
                                </div>
                            </div>

                            {/* Right Content Area (Personalization Settings Panel) */}
                            <div className="flex-1 flex flex-col overflow-y-auto px-6 sm:px-8 py-6">

                                {/* Active Tab View Switcher */}
                                {activeTab === 'Personalization' ? (
                                    <div className="space-y-6">
                                        {/* Header */}
                                        <div>
                                            <h1 className="text-2xl sm:text-3xl font-semibold leading-snug">Personalization</h1>
                                        </div>

                                        {/* Desktop Monitor Preview Box */}
                                        <div className="flex flex-col lg:flex-row items-center gap-6 p-5 rounded-xl bg-black/15 dark:bg-white/5 border border-white/5">
                                            {/* Interactive Visual Preview Monitor Mockup */}
                                            <div className="w-full max-w-sm flex-shrink-0 flex flex-col items-center">
                                                {/* Monitor Bezel */}
                                                <div className="relative w-full aspect-[16/10] bg-neutral-900 p-2.5 rounded-xl border border-neutral-700/60 shadow-inner overflow-hidden">
                                                    {/* Inside Monitor Screen */}
                                                    <div
                                                        className="w-full h-full rounded bg-cover bg-center relative overflow-hidden flex flex-col justify-between"
                                                        style={{ backgroundImage: `url(${currentTheme.wallpaper})` }}
                                                    >
                                                        {/* Accent Glow backdrop indicator */}
                                                        <div className="absolute inset-0 bg-gradient-to-t from-black/40 via-transparent to-transparent pointer-events-none" />

                                                        {/* Simulated Settings App inside preview screen */}
                                                        <div className="absolute top-4 left-4 w-28 aspect-[1.4] rounded bg-neutral-900/90 backdrop-blur-md border border-white/20 p-1 flex flex-col justify-between shadow-lg transform scale-100 hover:scale-105 transition-transform">
                                                            <div className="flex items-center justify-between border-b border-white/10 pb-0.5">
                                                                <span className="text-[5px] scale-90 font-bold block origin-left text-neutral-300">Settings Preview</span>
                                                                <div className="flex gap-0.5">
                                                                    <div className="w-1 h-1 bg-white/20 rounded-full"></div>
                                                                    <div className="w-1 h-1 bg-white/20 rounded-full"></div>
                                                                </div>
                                                            </div>
                                                            <div className="flex-1 flex gap-1 py-1">
                                                                <div className="w-7 bg-white/5 rounded-sm p-0.5 space-y-0.5">
                                                                    <div className="h-0.5 w-full bg-white/30 rounded-full"></div>
                                                                    <div className="h-0.5 w-4 bg-white/25 rounded-full"></div>
                                                                </div>
                                                                <div className="flex-1 bg-white/10 rounded-sm p-1 space-y-1">
                                                                    <div className="h-1 w-full rounded" style={{ backgroundColor: currentAccentColor }}></div>
                                                                    <div className="h-0.5 w-6 bg-white/45 rounded-full"></div>
                                                                    <div className="grid grid-cols-3 gap-0.5">
                                                                        <div className="h-1.5 bg-white/20 rounded-sm"></div>
                                                                        <div className="h-1.5 bg-white/20 rounded-sm"></div>
                                                                        <div className="h-1.5 bg-white/20 rounded-sm"></div>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </div>

                                                        {/* Tiny desktop shortcuts */}
                                                        <div className="absolute top-2 right-2 flex flex-col gap-1 items-end pointer-events-none">
                                                            <div className="w-2 h-2 bg-white/30 rounded-sm" />
                                                            <div className="w-2 h-2 bg-white/30 rounded-sm" />
                                                            <div className="w-2 h-2 bg-white/30 rounded-sm" />
                                                        </div>

                                                        {/* Tiny Simulated Taskbar */}
                                                        <div className="w-full bg-neutral-950/70 backdrop-blur-md h-3.5 flex items-center justify-between px-1.5 text-[5px]">
                                                            <div className="flex items-center gap-0.5">
                                                                <div className="w-1.5 h-1.5 bg-sky-500 rounded-sm" />
                                                            </div>
                                                            <div className="flex gap-1">
                                                                <div className="w-1 h-1 bg-white/80 rounded-full" />
                                                                <div className="w-1.5 h-1 bg-white/60 rounded-sm" />
                                                                <div className="w-1 h-1 bg-white/80 rounded-full" />
                                                            </div>
                                                            <div className="text-white opacity-80 font-mono text-[4px]">10:10 PM</div>
                                                        </div>
                                                    </div>
                                                </div>
                                                {/* Monitor Stand */}
                                                <div className="w-14 h-4 bg-neutral-800 border-x border-neutral-700"></div>
                                                <div className="w-24 h-1.5 bg-neutral-800 rounded-t-lg"></div>
                                            </div>

                                            {/* Right detail panel for Preview */}
                                            <div className="flex-1 space-y-2 text-xs">
                                                <h4 className="font-semibold text-sm">Theme Live Simulator</h4>
                                                <p className="opacity-75 leading-relaxed">
                                                    This preview box simulates the active appearance of your desktop screen, windows accent glows, and theme color choices.
                                                </p>
                                                <div className="pt-3 flex flex-wrap gap-2">
                                                    <span className="px-2 py-1 rounded bg-white/5 border border-white/5 flex items-center gap-1.5">
                                                        <span className="w-2.5 h-2.5 rounded-full inline-block" style={{ backgroundColor: currentAccentColor }} />
                                                        Accent Color
                                                    </span>
                                                    <span className="px-2 py-1 rounded bg-white/5 border border-white/5 capitalize">
                                                        Mode: {currentTheme.mode}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>

                                        {/* Quick Theme Selection Grid */}
                                        <div className="space-y-3">
                                            <h2 className="text-sm font-semibold opacity-90">Select a theme to apply</h2>

                                            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
                                                {THEMES.map((theme) => {
                                                    const isSelected = currentTheme.id === theme.id;
                                                    return (
                                                        <button
                                                            key={theme.id}
                                                            onClick={() => handleThemeChange(theme)}
                                                            className={`group relative aspect-[4/3] rounded-lg overflow-hidden border transition-all ${isSelected
                                                                    ? 'ring-2 ring-sky-500 border-transparent shadow-lg'
                                                                    : 'border-white/10 hover:border-white/20 hover:scale-[1.03]'
                                                                }`}
                                                            title={theme.name}
                                                        >
                                                            <img
                                                                src={theme.thumbnail}
                                                                alt={theme.name}
                                                                className="w-full h-full object-cover transition duration-300 group-hover:scale-105"
                                                            />
                                                            {/* Inner visual elements replicating the mini indicators */}
                                                            <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-transparent flex items-end p-1.5">
                                                                <span className="text-[10px] text-white font-medium truncate w-full text-left">
                                                                    {theme.name.split(' ')[0]}
                                                                </span>
                                                            </div>

                                                            {/* Tiny selection tick mark indicator in bottom center */}
                                                            {isSelected && (
                                                                <div className="absolute top-1.5 right-1.5 bg-sky-500 text-white rounded-full p-0.5">
                                                                    <Check className="w-2.5 h-2.5" />
                                                                </div>
                                                            )}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        </div>

                                        {/* Dynamic List of Options */}
                                        <div className="space-y-2">
                                            {filteredPersonalizationItems.map((item) => {
                                                const isExpanded = expandedSection === item.id;
                                                return (
                                                    <div
                                                        key={item.id}
                                                        className={`rounded-lg transition border ${currentTheme.mode === 'dark'
                                                                ? 'bg-[#2b2b2f]/40 hover:bg-[#2b2b2f]/60 border-white/5'
                                                                : 'bg-[#e2e2e8]/40 hover:bg-[#e2e2e8]/60 border-neutral-300/30'
                                                            }`}
                                                    >
                                                        <button
                                                            onClick={() => toggleSection(item.id)}
                                                            className="w-full flex items-center justify-between p-3.5 text-left focus:outline-none"
                                                        >
                                                            <div className="flex items-center gap-4">
                                                                <div className="p-2 rounded bg-black/10 dark:bg-white/5">
                                                                    <item.icon className="w-5 h-5" style={{ color: currentAccentColor }} />
                                                                </div>
                                                                <div>
                                                                    <h3 className="text-xs sm:text-sm font-semibold">{item.title}</h3>
                                                                    <p className="text-[11px] opacity-70 leading-normal">{item.desc}</p>
                                                                </div>
                                                            </div>
                                                            <div className="opacity-70">
                                                                {isExpanded ? <ChevronDown className="w-4 h-4" /> : <ChevronRight className="w-4 h-4" />}
                                                            </div>
                                                        </button>

                                                        {/* Expanded sub-menus */}
                                                        {isExpanded && (
                                                            <div className="px-4 pb-4 border-t border-white/5">
                                                                {item.details}
                                                            </div>
                                                        )}
                                                    </div>
                                                );
                                            })}

                                            {filteredPersonalizationItems.length === 0 && (
                                                <div className="text-center py-8 text-neutral-400 text-xs">
                                                    No settings match "{searchQuery}"
                                                </div>
                                            )}
                                        </div>

                                    </div>
                                ) : (
                                    // Generic Tab Content Mock
                                    <div className="h-full flex flex-col items-center justify-center text-center max-w-md mx-auto space-y-4 py-12">
                                        <div className="p-4 rounded-full bg-white/5 text-sky-500">
                                            <Laptop className="w-10 h-10" />
                                        </div>
                                        <div>
                                            <h2 className="text-lg font-semibold">{activeTab} Section</h2>
                                            <p className="text-xs text-neutral-400 mt-1">
                                                In this fully functional mockup app, the <strong>Personalization</strong> panel is fully fleshed out with customizable widgets. Click "Personalization" in the left sidebar to change themes, colors, and wallpapers!
                                            </p>
                                        </div>
                                        <button
                                            onClick={() => setActiveTab('Personalization')}
                                            className="px-4 py-2 text-xs font-semibold rounded-md text-white transition hover:brightness-110"
                                            style={{ backgroundColor: currentAccentColor }}
                                        >
                                            Return to Personalization
                                        </button>
                                    </div>
                                )}

                            </div>

                        </div>
                    </div>
                ) : (
                    /* Closed App Simulator Button */
                    <div className="flex flex-col items-center gap-4 bg-black/60 backdrop-blur-md px-6 py-5 rounded-xl border border-white/10 shadow-2xl text-center text-white max-w-sm">
                        <h3 className="font-semibold text-sm">Settings Window Closed</h3>
                        <p className="text-xs opacity-70">You can reopen the Settings panel to explore and customize the Windows 11 themes!</p>
                        <button
                            onClick={() => {
                                setIsOpen(true);
                                setActiveTab('Personalization');
                            }}
                            className="px-4 py-2 rounded font-semibold text-xs transition hover:opacity-90 w-full"
                            style={{ backgroundColor: currentAccentColor }}
                        >
                            Open Settings Window
                        </button>
                    </div>
                )}

            </div>

            {/* Windows 11 Bottom Taskbar */}
            <div className="w-full h-12 bg-neutral-950/80 backdrop-blur-xl border-t border-white/5 z-20 flex items-center justify-between px-3">
                {/* Empty left block for alignment */}
                <div className="w-24"></div>

                {/* Centered Taskbar Application Pins */}
                <div className="flex items-center gap-2">
                    {/* Windows Start Button */}
                    <button
                        className="w-10 h-10 hover:bg-white/10 rounded-md flex items-center justify-center transition group"
                        title="Start Menu"
                    >
                        <div className="grid grid-cols-2 gap-0.5 w-4 h-4 transition group-hover:scale-105">
                            <div className="w-1.5 h-1.5 rounded-sm bg-sky-500" />
                            <div className="w-1.5 h-1.5 rounded-sm bg-sky-500" />
                            <div className="w-1.5 h-1.5 rounded-sm bg-sky-500" />
                            <div className="w-1.5 h-1.5 rounded-sm bg-sky-500" />
                        </div>
                    </button>

                    {/* Active Settings Pin */}
                    <button
                        onClick={() => {
                            setIsOpen(true);
                            setActiveTab('Personalization');
                        }}
                        className="w-10 h-10 hover:bg-white/10 rounded-md flex flex-col items-center justify-center transition relative"
                        title="Settings"
                    >
                        <Paintbrush className="w-5 h-5 text-sky-400" />
                        {isOpen && (
                            <span className="absolute bottom-1 w-1.5 h-0.5 bg-sky-400 rounded-full" />
                        )}
                    </button>
                </div>

                {/* Right side System Icons & Date/Time block */}
                <div className="flex items-center gap-2 text-right font-sans pr-2">
                    {/* Quick status icons */}
                    <div className="hidden sm:flex items-center gap-2.5 text-xs text-white/80 mr-2">
                        <Globe className="w-3.5 h-3.5" />
                        <span className="text-[10px]">ENG</span>
                    </div>

                    {/* Calendar & Time */}
                    <div className="flex flex-col items-end text-white text-[11px] leading-tight px-2 py-1 rounded hover:bg-white/10 transition cursor-default">
                        <span>{time}</span>
                        <span className="text-[9px] opacity-75">{date}</span>
                    </div>
                </div>
            </div>
        </div>
    );
}

// Inline SVG component to replicate the specific "Windows Background Image" option icon perfectly
function ImageSectionIcon(props) {
    return (
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            {...props}
        >
            <rect width="18" height="18" x="3" y="3" rx="2" ry="2" />
            <circle cx="9" cy="9" r="2" />
            <path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21" />
        </svg>
    );
}