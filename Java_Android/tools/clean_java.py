import os
import sys

def clean_whitespace_in_file(file_path):
    """Read the Java file, remove trailing whitespace/tabs from each line, and overwrite it."""
    try:
        # Open the file with UTF-8 encoding to read its contents
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        cleaned_lines = []
        for line in lines:
            # rstrip() removes trailing spaces, tabs, and newlines at the end of each line,
            # then we append a standard newline character (\n).
            cleaned_line = line.rstrip() + '\n'
            cleaned_lines.append(cleaned_line)
        
        # If the original file content didn't end with a newline, 
        # remove the extra newline added to the very last line to preserve the original file ending.
        if lines and not lines[-1].endswith('\n'):
            if cleaned_lines:
                cleaned_lines[-1] = cleaned_lines[-1].rstrip('\n')
        
        # Overwrite the file with the cleaned content
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(cleaned_lines)
            
        print(f"Processed: {file_path}")
    except Exception as e:
        print(f"Error processing file {file_path}: {e}")

def clean_java_files_in_directory(root_dir):
    """Recursively scan the directory to find and process all .java files."""
    count = 0
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith('.java'):
                file_path = os.path.join(dirpath, filename)
                clean_whitespace_in_file(file_path)
                count += 1
                
    print(f"\nDone! Cleaned whitespace for a total of {count} Java file(s).")

if __name__ == "__main__":
    # If a command-line argument is passed, use it; otherwise, default to the current directory ("./")
    if len(sys.argv) > 1:
        target_directory = sys.argv[1]
    else:
        target_directory = "."
    
    if os.path.isdir(target_directory):
        print(f"Scanning directory: {os.path.abspath(target_directory)}")
        clean_java_files_in_directory(target_directory)
    else:
        print(f"Error: '{target_directory}' is not a valid directory or does not exist!")