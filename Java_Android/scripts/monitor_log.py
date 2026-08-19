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

def load_keywords_from_config(config_filename="filter.config"):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    config_path = os.path.join(current_dir, config_filename)
    default_keywords = ["crash", "failed", "exception", "fatal"]

    if not os.path.exists(config_path):
        try:
            with open(config_path, 'w', encoding='utf-8') as f:
                f.write("# Thêm các từ khóa cần lọc bên dưới, mỗi từ khóa một dòng.\n")
                f.write("# Hỗ trợ bỏ qua chữ hoa/chữ thường.\n")
                for kw in default_keywords:
                    f.write(f"{kw}\n")
            return default_keywords
        except Exception:
            return default_keywords

    keywords = []
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            for line in f:
                kw = line.strip()
                if kw and not kw.startswith("#"):
                    keywords.append(kw)
    except Exception:
        pass

    return keywords if keywords else default_keywords

def colorize_line(line):
    """Phân tích dòng log và trả về chuỗi đã được bọc mã màu"""
    line_clean = line.strip()
    match = LOG_PATTERN.search(line_clean)
    
    if match:
        level = match.group(1)
        color = COLORS.get(level, COLORS["RESET"])
        return f"{color}{line_clean}{COLORS['RESET']}"
    
    return f"\033[35m{line_clean}{COLORS['RESET']}"

def monitor_log(lnk_filename, keywords):
    current_dir = os.path.dirname(os.path.abspath(__file__))
    lnk_path = os.path.join(current_dir, lnk_filename)

    if not os.path.exists(lnk_path):
        print(f"\033[31m[LỖI] Không tìm thấy {lnk_filename}. Hãy chạy logcat.bat trước.\033[0m")
        return

    log_path = get_target_from_lnk(lnk_path)
    if not log_path or not os.path.exists(log_path):
        print(f"\033[31m[LỖI] File log gốc không tồn tại: {log_path}\033[0m")
        return

    # Tạo đường dẫn cho file filter: thêm tiền tố "filter_" vào tên file gốc
    log_dir = os.path.dirname(log_path)
    log_basename = os.path.basename(log_path)
    filter_log_path = os.path.join(log_dir, f"filter_{log_basename}")

    print("=" * 60)
    print(f" Đang đọc file gốc:   \033[36m{log_basename}\033[0m")
    print(f" Đang ghi file filter: \033[32m{os.path.basename(filter_log_path)}\033[0m")
    print(f" Từ khóa bộ lọc:     \033[33m{keywords}\033[0m")
    print(" Nhấn Ctrl+C để dừng.")
    print("=" * 60)

    keywords_lower = [kw.lower() for kw in keywords]

    try:
        # Mở song song 2 file: đọc file log gốc ('r') và ghi nối tiếp vào file filter ('a')
        with open(log_path, 'r', encoding='utf-8', errors='ignore') as f_in, \
             open(filter_log_path, 'a', encoding='utf-8') as f_out:
            
            # 1. Quét nội dung hiện có
            for line in f_in:
                if any(kw in line.lower() for kw in keywords_lower):
                    print(colorize_line(line))
                    f_out.write(line)
            f_out.flush() # Đẩy dữ liệu từ buffer xuống thẳng ổ cứng
            
            # 2. Vòng lặp chờ log mới (Real-time monitoring)
            while True:
                line = f_in.readline()
                if not line:
                    time.sleep(0.5) 
                    continue
                
                if any(kw in line.lower() for kw in keywords_lower):
                    print(colorize_line(line))
                    f_out.write(line)
                    f_out.flush() # Ghi ngay lập tức mỗi khi có lỗi mới

    except KeyboardInterrupt:
        print("\n\033[33m[THÔNG BÁO] Đã dừng theo dõi log.\033[0m")
    except Exception as e:
        print(f"\n\033[31m[LỖI] Đã xảy ra lỗi: {e}\033[0m")

if __name__ == "__main__":
    LNK_FILE = "latest_log.lnk"
    CONFIG_FILE = "filter_logcat.config"
    
    FILTER_KEYWORDS = load_keywords_from_config(CONFIG_FILE)
    monitor_log(LNK_FILE, FILTER_KEYWORDS)