# Directory Summary: `messages/audio/`

This directory contains documentation for the `com.google.android.gms.nearby.messages.audio` package.
**Status**: Deprecated. Nearby Messages no longer supports audio.

## File Details

### `AudioBytes.md`
- **Description**: Represents a message sent over near-ultrasound audio.
- **Key Constraints**: Payload is limited to `MAX_SIZE` (10 bytes).
- **Key Methods**:
    - `from(Message message)`: Converts a `Message` object to `AudioBytes`.
    - `toMessage()`: Converts `AudioBytes` back to a `Message` object for publishing.
    - `getBytes()`: Retrieves the raw byte array payload.
- **Deprecation Note**: Explicitly marked as deprecated as the audio medium is no longer supported.

### `README.md`
- **Description**: Package summary file.
- **Content**: Lists the `AudioBytes` class and highlights its deprecated status.
