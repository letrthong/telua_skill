import os
import time
import re
import sys

# Kích hoạt hỗ trợ ANSI Escape Codes cho Windows CMD/PowerShell
os.system("")

# Kiểm tra xem user đã cài pywin32 chưa, nếu chưa thì hướng dẫn cài đặt
try:
    import win32com.client
except ImportError:
    print("\033[31m[LỖI] Thiếu thư viện 'pywin32'!\033[0m")
    print("Vui lòng mở Command Prompt (CMD) và chạy lệnh sau để cài đặt:")
    print("\n    \033[33mpip install pywin32\033[0m\n")
    print("Sau khi cài đặt xong, hãy chạy lại công cụ bằng lệnh:")
    print("\n    \033[32mpython monitor_log.py\033[0m\n")
    sys.exit(1)

# Bảng mã màu ANSI
COLORS = {
    "V": "\033[38;5;244m",   # Gray (Verbose)
    "D": "\033[36m",         # Cyan (Debug)
    "I": "\033[32m",         # Green (Info)
    "W": "\033[33m",         # Yellow (Warning)
    "E": "\033[31m",         # Red (Error)
    "F": "\033[41m\033[37m", # White text on Red background (Fatal)
    "RESET": "\033[0m"       # Reset màu
}

# Regex nhận diện định dạng logcat threadtime
LOG_PATTERN = re.compile(r"^\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3}\s+\d+\s+\d+\s+([VDIWEF])\s")

def get_target_from_lnk(lnk_path):
    try:
        shell = win32com.client.Dispatch("WScript.Shell")
        shortcut = shell.CreateShortCut(lnk_path)
        return shortcut.Targetpath
    except Exception as e:
        print(f"[LỖI] Không thể đọc file shortcut: {e}")
        return None

def load_filters_from_config(config_filename="filter_logcat.config"):
    """Đọc filter_logcat.config (JSON) và trả về danh sách nhóm đã lọc.

    Mỗi nhóm chỉ được giữ lại nếu "Enable": true.
    Trong mỗi nhóm, chỉ giữ các keyword có "Enable": true.
    Trả về dạng:
        [
            {"description": "...", "keywords": ["kw1", "kw2", ...]},
            ...
        ]
    """
    import json

    current_dir = os.path.dirname(os.path.abspath(__file__))
    config_path = os.path.join(current_dir, config_filename)
    default_groups = [
        {"description": "system crash", "keywords": ["crash", "failed", "exception", "fatal"]}
    ]

    if not os.path.exists(config_path):
        print(f"\033[33m[THÔNG BÁO] Không tìm thấy {config_filename}, dùng từ khóa mặc định.\033[0m")
        return default_groups

    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except json.JSONDecodeError as e:
        print("\033[41m\033[37m" + "=" * 60 + "\033[0m")
        print("\033[41m\033[37m[LỖI] " + config_filename + " sai định dạng JSON!\033[0m")
        print(f"\033[31m  Vị trí lỗi: dòng {e.lineno}, cột {e.colno}\033[0m")
        print(f"\033[31m  Chi tiết:   {e.msg}\033[0m")
        print("\033[41m\033[37m" + "=" * 60 + "\033[0m")
        print("\033[33m[THÔNG BÁO] Vui lòng sửa lại file config rồi chạy lại.\033[0m")
        sys.exit(1)
    except Exception as e:
        print(f"\033[31m[LỖI] Không đọc được {config_filename}: {e}\033[0m")
        print("\033[33m[THÔNG BÁO] Dùng từ khóa mặc định.\033[0m")
        return default_groups

    # Kiểm tra cấu trúc dữ liệu có đúng dạng danh sách nhóm không
    if not isinstance(data, list):
        print("\033[41m\033[37m" + "=" * 60 + "\033[0m")
        print("\033[41m\033[37m[LỖI] " + config_filename + " sai cấu trúc!\033[0m")
        print("\033[31m  Cấu trúc phải là một mảng (list) các nhóm, ví dụ: [ { ... }, { ... } ]\033[0m")
        print("\033[41m\033[37m" + "=" * 60 + "\033[0m")
        sys.exit(1)

    groups = []
    for idx, group in enumerate(data):
        # Kiểm tra mỗi nhóm có phải object không
        if not isinstance(group, dict):
            print(f"\033[31m[LỖI] Nhóm thứ {idx + 1} không phải object hợp lệ.\033[0m")
            sys.exit(1)

        # Bỏ qua nhóm bị tắt
        if not group.get("Enable", True):
            continue

        description = group.get("Description", "Không tên")
        keywords = []

        for item in group.get("filters", []):
            # Hỗ trợ cả 2 dạng: chuỗi đơn hoặc object {keyword, Enable}
            if isinstance(item, str):
                kw = item
                enabled = True
            else:
                kw = item.get("keyword", "")
                enabled = item.get("Enable", True)

            # Chỉ giữ keyword được bật
            if kw and enabled:
                keywords.append(kw)

        if keywords:
            groups.append({"description": description, "keywords": keywords})

    if not groups:
        print("\033[33m[THÔNG BÁO] Không có nhóm nào được bật (Enable: true) trong " + config_filename + ".\033[0m")
        print("\033[33m[THÔNG BÁO] Đang dùng từ khóa mặc định: " + ", ".join(default_groups[0]["keywords"]) + "\033[0m")
        print("\033[33m[THÔNG BÁO] Hãy mở " + config_filename + " và đổi 'Enable' thành true cho nhóm bạn muốn lọc.\033[0m")
        return default_groups

    return groups

def colorize_line(line):
    """Phân tích dòng log và trả về chuỗi đã được bọc mã màu"""
    line_clean = line.strip()
    match = LOG_PATTERN.search(line_clean)
    
    if match:
        level = match.group(1)
        color = COLORS.get(level, COLORS["RESET"])
        return f"{color}{line_clean}{COLORS['RESET']}"
    
    return f"\033[35m{line_clean}{COLORS['RESET']}"

def compile_keyword_patterns(keywords):
    """Chuyển danh sách keyword thành danh sách pattern để khớp.

    - Keyword có '*' (wildcard) -> biên dịch thành regex (vd: CAR.AUDIO.*)
    - Keyword không có '*' -> giữ nguyên chuỗi để khớp nhanh (substring)
    """
    patterns = []
    for kw in keywords:
        if '*' in kw:
            # Chuyển wildcard '*' thành regex '.*', escape các ký tự đặc biệt khác
            regex = re.escape(kw).replace(r'\*', '.*')
            patterns.append(("regex", re.compile(regex, re.IGNORECASE)))
        else:
            patterns.append(("plain", kw.lower()))
    return patterns

def line_matches(line_lower, patterns):
    """Kiểm tra dòng log có khớp bất kỳ pattern nào không."""
    for kind, pat in patterns:
        if kind == "regex":
            if pat.search(line_lower):
                return True
        else:
            if pat in line_lower:
                return True
    return False

def monitor_log(lnk_filename, groups):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    lnk_path = os.path.join(current_dir, lnk_filename)

    # Gom tất cả keyword đã được bật (từ các nhóm Enable:true và keyword Enable:true)
    keywords = []
    for group in groups:
        keywords.extend(group["keywords"])
    patterns = compile_keyword_patterns(keywords)

    # Chờ cho đến khi latest_log.lnk xuất hiện (logcat.bat chưa chạy hoặc chưa tạo link)
    print("=" * 60)
    print(f" Từ khóa bộ lọc:     \033[33m{keywords}\033[0m")
    print(" Đang chờ logcat.bat tạo latest_log.lnk ...")
    print(" Nhấn Ctrl+C để dừng.")
    print("=" * 60)

    while not os.path.exists(lnk_path):
        time.sleep(1)

    print(f"\033[32m[OK] Đã tìm thấy {lnk_filename}.\033[0m")

    def open_current_log():
        """Giải mã .lnk, mở file log gốc và file filter tương ứng."""
        log_path = get_target_from_lnk(lnk_path)
        if not log_path or not os.path.exists(log_path):
            return None, None, None

        log_dir = os.path.dirname(log_path)
        log_basename = os.path.basename(log_path)
        filter_log_path = os.path.join(log_dir, f"filter_{log_basename}")

        # Mở với quyền chia sẻ đọc để tránh lỗi sharing violation khi logcat.bat đang ghi
        f_in = open(log_path, 'r', encoding='utf-8', errors='ignore')
        f_out = open(filter_log_path, 'a', encoding='utf-8')
        return log_path, f_in, f_out

    try:
        log_path, f_in, f_out = open_current_log()
        if not f_in:
            print(f"\033[31m[LỖI] Không tìm thấy file log. Hãy chạy logcat.bat trước.\033[0m")
            return

        print(f" Đang đọc file gốc:   \033[36m{os.path.basename(log_path)}\033[0m")
        print(f" Đang ghi file filter: \033[32m{os.path.basename(f_out.name)}\033[0m")

        # 1. Quét nội dung hiện có (xử lý cả trường hợp file trống lúc khởi động)
        for line in f_in:
            if line_matches(line.lower(), patterns):
                print(colorize_line(line))
                f_out.write(line)
        f_out.flush()

        # 2. Vòng lặp chờ log mới (Real-time monitoring) + tự refresh khi logcat.bat tạo file mới
        last_check = time.time()
        while True:
            # Định kỳ kiểm tra lại .lnk: nếu logcat.bat đã chuyển sang file mới thì mở file mới
            if time.time() - last_check >= 2:
                last_check = time.time()
                new_log_path = get_target_from_lnk(lnk_path)
                if new_log_path and new_log_path != log_path:
                    f_in.close()
                    f_out.close()
                    log_path, f_in, f_out = open_current_log()
                    if f_in:
                        # Xóa terminal để bắt đầu phiên log mới với màn hình sạch
                        os.system("cls" if os.name == "nt" else "clear")
                        print(f"\n\033[36m[REFRESH] Chuyển sang file log mới: {os.path.basename(log_path)}\033[0m")
                        # Quét nội dung đã có của file mới
                        for line in f_in:
                            if line_matches(line.lower(), patterns):
                                print(colorize_line(line))
                                f_out.write(line)
                        f_out.flush()

            line = f_in.readline()
            if not line:
                time.sleep(0.5)
                continue

            if line_matches(line.lower(), patterns):
                print(colorize_line(line))
                f_out.write(line)
                f_out.flush()  # Ghi ngay lập tức mỗi khi có lỗi mới

    except KeyboardInterrupt:
        print("\n\033[33m[THÔNG BÁO] Đã dừng theo dõi log.\033[0m")
    except Exception as e:
        print(f"\n\033[31m[LỖI] Đã xảy ra lỗi: {e}\033[0m")
    finally:
        # Đóng file để tránh rò rỉ handle khi thoát (Ctrl+C hoặc lỗi)
        try:
            f_out.close()
        except Exception:
            pass
        try:
            f_in.close()
        except Exception:
            pass

def analyze_log_file(log_path, groups):
    """Phân tích lại một file log cũ (không theo dõi real-time).

    Đọc toàn bộ file log, lọc theo keyword, in ra màn hình (tô màu)
    và ghi vào file filter_<tên log>.
    """
    if not os.path.exists(log_path):
        print(f"\033[31m[LỖI] Không tìm thấy file log: {log_path}\033[0m")
        return

    # Gom tất cả keyword đã được bật
    keywords = []
    for group in groups:
        keywords.extend(group["keywords"])
    patterns = compile_keyword_patterns(keywords)

    log_dir = os.path.dirname(log_path)
    log_basename = os.path.basename(log_path)
    filter_log_path = os.path.join(log_dir, f"filter_{log_basename}")

    print("=" * 60)
    print(f" Phân tích file log:   \033[36m{log_path}\033[0m")
    print(f" Từ khóa bộ lọc:       \033[33m{keywords}\033[0m")
    print(f" Ghi file filter:      \033[32m{filter_log_path}\033[0m")
    print("=" * 60)

    try:
        with open(log_path, 'r', encoding='utf-8', errors='ignore') as f_in, \
             open(filter_log_path, 'w', encoding='utf-8') as f_out:
            count = 0
            for line in f_in:
                if line_matches(line.lower(), patterns):
                    print(colorize_line(line))
                    f_out.write(line)
                    count += 1
            f_out.flush()
        print(f"\n\033[32m[OK] Đã phân tích xong. Số dòng khớp: {count}\033[0m")
    except Exception as e:
        print(f"\n\033[31m[LỖI] Đã xảy ra lỗi khi phân tích: {e}\033[0m")

if __name__ == "__main__":
    LNK_FILE = "latest_log.lnk"
    CONFIG_FILE = "filter_logcat.config"

    FILTER_GROUPS = load_filters_from_config(CONFIG_FILE)

    # Nếu truyền tham số file path -> phân tích lại file log cũ
    if len(sys.argv) > 1:
        analyze_log_file(sys.argv[1], FILTER_GROUPS)
    else:
        # Không truyền -> logic hiện tại: theo dõi real-time qua .lnk
        monitor_log(LNK_FILE, FILTER_GROUPS)