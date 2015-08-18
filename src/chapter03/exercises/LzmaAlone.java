package chapter03.exercises;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

interface ICodeProgress
{
	public void SetProgress(long inSize, long outSize);
}

class Base {
	public static final int kNumRepDistances = 4;
	public static final int kNumStates = 12;

	public static final int StateInit() {
		return 0;
	}

	public static final int StateUpdateChar(int index) {
		if (index < 4)
			return 0;
		if (index < 10)
			return index - 3;
		return index - 6;
	}

	public static final int StateUpdateMatch(int index) {
		return (index < 7 ? 7 : 10);
	}

	public static final int StateUpdateRep(int index) {
		return (index < 7 ? 8 : 11);
	}

	public static final int StateUpdateShortRep(int index) {
		return (index < 7 ? 9 : 11);
	}

	public static final boolean StateIsCharState(int index) {
		return index < 7;
	}

	public static final int kNumPosSlotBits = 6;
	public static final int kDicLogSizeMin = 0;
	// public static final int kDicLogSizeMax = 28;
	// public static final int kDistTableSizeMax = kDicLogSizeMax * 2;

	public static final int kNumLenToPosStatesBits = 2; // it's for speed
	// optimization
	public static final int kNumLenToPosStates = 1 << kNumLenToPosStatesBits;

	public static final int kMatchMinLen = 2;

	public static final int GetLenToPosState(int len) {
		len -= kMatchMinLen;
		if (len < kNumLenToPosStates)
			return len;
		return (int) (kNumLenToPosStates - 1);
	}

	public static final int kNumAlignBits = 4;
	public static final int kAlignTableSize = 1 << kNumAlignBits;
	public static final int kAlignMask = (kAlignTableSize - 1);

	public static final int kStartPosModelIndex = 4;
	public static final int kEndPosModelIndex = 14;
	public static final int kNumPosModels = kEndPosModelIndex
			- kStartPosModelIndex;

	public static final int kNumFullDistances = 1 << (kEndPosModelIndex / 2);

	public static final int kNumLitPosStatesBitsEncodingMax = 4;
	public static final int kNumLitContextBitsMax = 8;

	public static final int kNumPosStatesBitsMax = 4;
	public static final int kNumPosStatesMax = (1 << kNumPosStatesBitsMax);
	public static final int kNumPosStatesBitsEncodingMax = 4;
	public static final int kNumPosStatesEncodingMax = (1 << kNumPosStatesBitsEncodingMax);

	public static final int kNumLowLenBits = 3;
	public static final int kNumMidLenBits = 3;
	public static final int kNumHighLenBits = 8;
	public static final int kNumLowLenSymbols = 1 << kNumLowLenBits;
	public static final int kNumMidLenSymbols = 1 << kNumMidLenBits;
	public static final int kNumLenSymbols = kNumLowLenSymbols
			+ kNumMidLenSymbols + (1 << kNumHighLenBits);
	public static final int kMatchMaxLen = kMatchMinLen + kNumLenSymbols - 1;
}

class BinTree extends InWindow {
	int _cyclicBufferPos;
	int _cyclicBufferSize = 0;
	int _matchMaxLen;

	int[] _son;
	int[] _hash;

	int _cutValue = 0xFF;
	int _hashMask;
	int _hashSizeSum = 0;

	boolean HASH_ARRAY = true;

	static final int kHash2Size = 1 << 10;
	static final int kHash3Size = 1 << 16;
	static final int kBT2HashSize = 1 << 16;
	static final int kStartMaxLen = 1;
	static final int kHash3Offset = kHash2Size;
	static final int kEmptyHashValue = 0;
	static final int kMaxValForNormalize = (1 << 30) - 1;

	int kNumHashDirectBytes = 0;
	int kMinMatchCheck = 4;
	int kFixHashSize = kHash2Size + kHash3Size;

	public void SetType(int numHashBytes) {
		HASH_ARRAY = (numHashBytes > 2);
		if (HASH_ARRAY) {
			kNumHashDirectBytes = 0;
			kMinMatchCheck = 4;
			kFixHashSize = kHash2Size + kHash3Size;
		} else {
			kNumHashDirectBytes = 2;
			kMinMatchCheck = 2 + 1;
			kFixHashSize = 0;
		}
	}

	public void Init() throws IOException {
		super.Init();
		for (int i = 0; i < _hashSizeSum; i++)
			_hash[i] = kEmptyHashValue;
		_cyclicBufferPos = 0;
		ReduceOffsets(-1);
	}

	public void MovePos() throws IOException {
		if (++_cyclicBufferPos >= _cyclicBufferSize)
			_cyclicBufferPos = 0;
		super.MovePos();
		if (_pos == kMaxValForNormalize)
			Normalize();
	}

	public boolean Create(int historySize, int keepAddBufferBefore,
			int matchMaxLen, int keepAddBufferAfter) {
		if (historySize > kMaxValForNormalize - 256)
			return false;
		_cutValue = 16 + (matchMaxLen >> 1);

		int windowReservSize = (historySize + keepAddBufferBefore + matchMaxLen + keepAddBufferAfter) / 2 + 256;

		super.Create(historySize + keepAddBufferBefore, matchMaxLen
				+ keepAddBufferAfter, windowReservSize);

		_matchMaxLen = matchMaxLen;

		int cyclicBufferSize = historySize + 1;
		if (_cyclicBufferSize != cyclicBufferSize)
			_son = new int[(_cyclicBufferSize = cyclicBufferSize) * 2];

		int hs = kBT2HashSize;

		if (HASH_ARRAY) {
			hs = historySize - 1;
			hs |= (hs >> 1);
			hs |= (hs >> 2);
			hs |= (hs >> 4);
			hs |= (hs >> 8);
			hs >>= 1;
			hs |= 0xFFFF;
			if (hs > (1 << 24))
				hs >>= 1;
			_hashMask = hs;
			hs++;
			hs += kFixHashSize;
		}
		if (hs != _hashSizeSum)
			_hash = new int[_hashSizeSum = hs];
		return true;
	}

	public int GetMatches(int[] distances) throws IOException {
		int lenLimit;
		if (_pos + _matchMaxLen <= _streamPos)
			lenLimit = _matchMaxLen;
		else {
			lenLimit = _streamPos - _pos;
			if (lenLimit < kMinMatchCheck) {
				MovePos();
				return 0;
			}
		}

		int offset = 0;
		int matchMinPos = (_pos > _cyclicBufferSize) ? (_pos - _cyclicBufferSize)
				: 0;
		int cur = _bufferOffset + _pos;
		int maxLen = kStartMaxLen; // to avoid items for len < hashSize;
		int hashValue, hash2Value = 0, hash3Value = 0;

		if (HASH_ARRAY) {
			int temp = CrcTable[_bufferBase[cur] & 0xFF]
					^ (_bufferBase[cur + 1] & 0xFF);
			hash2Value = temp & (kHash2Size - 1);
			temp ^= ((int) (_bufferBase[cur + 2] & 0xFF) << 8);
			hash3Value = temp & (kHash3Size - 1);
			hashValue = (temp ^ (CrcTable[_bufferBase[cur + 3] & 0xFF] << 5))
					& _hashMask;
		} else
			hashValue = ((_bufferBase[cur] & 0xFF) ^ ((int) (_bufferBase[cur + 1] & 0xFF) << 8));

		int curMatch = _hash[kFixHashSize + hashValue];
		if (HASH_ARRAY) {
			int curMatch2 = _hash[hash2Value];
			int curMatch3 = _hash[kHash3Offset + hash3Value];
			_hash[hash2Value] = _pos;
			_hash[kHash3Offset + hash3Value] = _pos;
			if (curMatch2 > matchMinPos)
				if (_bufferBase[_bufferOffset + curMatch2] == _bufferBase[cur]) {
					distances[offset++] = maxLen = 2;
					distances[offset++] = _pos - curMatch2 - 1;
				}
			if (curMatch3 > matchMinPos)
				if (_bufferBase[_bufferOffset + curMatch3] == _bufferBase[cur]) {
					if (curMatch3 == curMatch2)
						offset -= 2;
					distances[offset++] = maxLen = 3;
					distances[offset++] = _pos - curMatch3 - 1;
					curMatch2 = curMatch3;
				}
			if (offset != 0 && curMatch2 == curMatch) {
				offset -= 2;
				maxLen = kStartMaxLen;
			}
		}

		_hash[kFixHashSize + hashValue] = _pos;

		int ptr0 = (_cyclicBufferPos << 1) + 1;
		int ptr1 = (_cyclicBufferPos << 1);

		int len0, len1;
		len0 = len1 = kNumHashDirectBytes;

		if (kNumHashDirectBytes != 0) {
			if (curMatch > matchMinPos) {
				if (_bufferBase[_bufferOffset + curMatch + kNumHashDirectBytes] != _bufferBase[cur
						+ kNumHashDirectBytes]) {
					distances[offset++] = maxLen = kNumHashDirectBytes;
					distances[offset++] = _pos - curMatch - 1;
				}
			}
		}

		int count = _cutValue;

		while (true) {
			if (curMatch <= matchMinPos || count-- == 0) {
				_son[ptr0] = _son[ptr1] = kEmptyHashValue;
				break;
			}
			int delta = _pos - curMatch;
			int cyclicPos = ((delta <= _cyclicBufferPos) ? (_cyclicBufferPos - delta)
					: (_cyclicBufferPos - delta + _cyclicBufferSize)) << 1;

			int pby1 = _bufferOffset + curMatch;
			int len = Math.min(len0, len1);
			if (_bufferBase[pby1 + len] == _bufferBase[cur + len]) {
				while (++len != lenLimit)
					if (_bufferBase[pby1 + len] != _bufferBase[cur + len])
						break;
				if (maxLen < len) {
					distances[offset++] = maxLen = len;
					distances[offset++] = delta - 1;
					if (len == lenLimit) {
						_son[ptr1] = _son[cyclicPos];
						_son[ptr0] = _son[cyclicPos + 1];
						break;
					}
				}
			}
			if ((_bufferBase[pby1 + len] & 0xFF) < (_bufferBase[cur + len] & 0xFF)) {
				_son[ptr1] = curMatch;
				ptr1 = cyclicPos + 1;
				curMatch = _son[ptr1];
				len1 = len;
			} else {
				_son[ptr0] = curMatch;
				ptr0 = cyclicPos;
				curMatch = _son[ptr0];
				len0 = len;
			}
		}
		MovePos();
		return offset;
	}

	public void Skip(int num) throws IOException {
		do {
			int lenLimit;
			if (_pos + _matchMaxLen <= _streamPos)
				lenLimit = _matchMaxLen;
			else {
				lenLimit = _streamPos - _pos;
				if (lenLimit < kMinMatchCheck) {
					MovePos();
					continue;
				}
			}

			int matchMinPos = (_pos > _cyclicBufferSize) ? (_pos - _cyclicBufferSize)
					: 0;
			int cur = _bufferOffset + _pos;

			int hashValue;

			if (HASH_ARRAY) {
				int temp = CrcTable[_bufferBase[cur] & 0xFF]
						^ (_bufferBase[cur + 1] & 0xFF);
				int hash2Value = temp & (kHash2Size - 1);
				_hash[hash2Value] = _pos;
				temp ^= ((int) (_bufferBase[cur + 2] & 0xFF) << 8);
				int hash3Value = temp & (kHash3Size - 1);
				_hash[kHash3Offset + hash3Value] = _pos;
				hashValue = (temp ^ (CrcTable[_bufferBase[cur + 3] & 0xFF] << 5))
						& _hashMask;
			} else
				hashValue = ((_bufferBase[cur] & 0xFF) ^ ((int) (_bufferBase[cur + 1] & 0xFF) << 8));

			int curMatch = _hash[kFixHashSize + hashValue];
			_hash[kFixHashSize + hashValue] = _pos;

			int ptr0 = (_cyclicBufferPos << 1) + 1;
			int ptr1 = (_cyclicBufferPos << 1);

			int len0, len1;
			len0 = len1 = kNumHashDirectBytes;

			int count = _cutValue;
			while (true) {
				if (curMatch <= matchMinPos || count-- == 0) {
					_son[ptr0] = _son[ptr1] = kEmptyHashValue;
					break;
				}

				int delta = _pos - curMatch;
				int cyclicPos = ((delta <= _cyclicBufferPos) ? (_cyclicBufferPos - delta)
						: (_cyclicBufferPos - delta + _cyclicBufferSize)) << 1;

				int pby1 = _bufferOffset + curMatch;
				int len = Math.min(len0, len1);
				if (_bufferBase[pby1 + len] == _bufferBase[cur + len]) {
					while (++len != lenLimit)
						if (_bufferBase[pby1 + len] != _bufferBase[cur + len])
							break;
					if (len == lenLimit) {
						_son[ptr1] = _son[cyclicPos];
						_son[ptr0] = _son[cyclicPos + 1];
						break;
					}
				}
				if ((_bufferBase[pby1 + len] & 0xFF) < (_bufferBase[cur + len] & 0xFF)) {
					_son[ptr1] = curMatch;
					ptr1 = cyclicPos + 1;
					curMatch = _son[ptr1];
					len1 = len;
				} else {
					_son[ptr0] = curMatch;
					ptr0 = cyclicPos;
					curMatch = _son[ptr0];
					len0 = len;
				}
			}
			MovePos();
		} while (--num != 0);
	}

	void NormalizeLinks(int[] items, int numItems, int subValue) {
		for (int i = 0; i < numItems; i++) {
			int value = items[i];
			if (value <= subValue)
				value = kEmptyHashValue;
			else
				value -= subValue;
			items[i] = value;
		}
	}

	void Normalize() {
		int subValue = _pos - _cyclicBufferSize;
		NormalizeLinks(_son, _cyclicBufferSize * 2, subValue);
		NormalizeLinks(_hash, _hashSizeSum, subValue);
		ReduceOffsets(subValue);
	}

	public void SetCutValue(int cutValue) {
		_cutValue = cutValue;
	}

	private static final int[] CrcTable = new int[256];

	static {
		for (int i = 0; i < 256; i++) {
			int r = i;
			for (int j = 0; j < 8; j++)
				if ((r & 1) != 0)
					r = (r >>> 1) ^ 0xEDB88320;
				else
					r >>>= 1;
			CrcTable[i] = r;
		}
	}
}

class BitTreeDecoder {
	short[] Models;
	int NumBitLevels;

	public BitTreeDecoder(int numBitLevels) {
		NumBitLevels = numBitLevels;
		Models = new short[1 << numBitLevels];
	}

	public void Init() {
		Decoder2.InitBitModels(Models);
	}

	public int Decode(Decoder2 rangeDecoder) throws java.io.IOException {
		int m = 1;
		for (int bitIndex = NumBitLevels; bitIndex != 0; bitIndex--)
			m = (m << 1) + rangeDecoder.DecodeBit(Models, m);
		return m - (1 << NumBitLevels);
	}

	public int ReverseDecode(Decoder2 rangeDecoder) throws java.io.IOException {
		int m = 1;
		int symbol = 0;
		for (int bitIndex = 0; bitIndex < NumBitLevels; bitIndex++) {
			int bit = rangeDecoder.DecodeBit(Models, m);
			m <<= 1;
			m += bit;
			symbol |= (bit << bitIndex);
		}
		return symbol;
	}

	public static int ReverseDecode(short[] Models, int startIndex,
			Decoder2 rangeDecoder, int NumBitLevels) throws java.io.IOException {
		int m = 1;
		int symbol = 0;
		for (int bitIndex = 0; bitIndex < NumBitLevels; bitIndex++) {
			int bit = rangeDecoder.DecodeBit(Models, startIndex + m);
			m <<= 1;
			m += bit;
			symbol |= (bit << bitIndex);
		}
		return symbol;
	}
}
class BitTreeEncoder
{
	short[] Models;
	int NumBitLevels;
	
	public BitTreeEncoder(int numBitLevels)
	{
		NumBitLevels = numBitLevels;
		Models = new short[1 << numBitLevels];
	}
	
	public void Init()
	{
		Decoder2.InitBitModels(Models);
	}
	
	public void Encode(Encoder2 rangeEncoder, int symbol) throws IOException
	{
		int m = 1;
		for (int bitIndex = NumBitLevels; bitIndex != 0; )
		{
			bitIndex--;
			int bit = (symbol >>> bitIndex) & 1;
			rangeEncoder.Encode(Models, m, bit);
			m = (m << 1) | bit;
		}
	}
	
	public void ReverseEncode(Encoder2 rangeEncoder, int symbol) throws IOException
	{
		int m = 1;
		for (int  i = 0; i < NumBitLevels; i++)
		{
			int bit = symbol & 1;
			rangeEncoder.Encode(Models, m, bit);
			m = (m << 1) | bit;
			symbol >>= 1;
		}
	}
	
	public int GetPrice(int symbol)
	{
		int price = 0;
		int m = 1;
		for (int bitIndex = NumBitLevels; bitIndex != 0; )
		{
			bitIndex--;
			int bit = (symbol >>> bitIndex) & 1;
			price += Encoder2.GetPrice(Models[m], bit);
			m = (m << 1) + bit;
		}
		return price;
	}
	
	public int ReverseGetPrice(int symbol)
	{
		int price = 0;
		int m = 1;
		for (int i = NumBitLevels; i != 0; i--)
		{
			int bit = symbol & 1;
			symbol >>>= 1;
			price += Encoder2.GetPrice(Models[m], bit);
			m = (m << 1) | bit;
		}
		return price;
	}
	
	public static int ReverseGetPrice(short[] Models, int startIndex,
			int NumBitLevels, int symbol)
	{
		int price = 0;
		int m = 1;
		for (int i = NumBitLevels; i != 0; i--)
		{
			int bit = symbol & 1;
			symbol >>>= 1;
			price += Encoder2.GetPrice(Models[startIndex + m], bit);
			m = (m << 1) | bit;
		}
		return price;
	}
	
	public static void ReverseEncode(short[] Models, int startIndex,
			Encoder2 rangeEncoder, int NumBitLevels, int symbol) throws IOException
	{
		int m = 1;
		for (int i = 0; i < NumBitLevels; i++)
		{
			int bit = symbol & 1;
			rangeEncoder.Encode(Models, startIndex + m, bit);
			m = (m << 1) | bit;
			symbol >>= 1;
		}
	}
}
class InWindow
{
	public byte[] _bufferBase; // pointer to buffer with data
	java.io.InputStream _stream;
	int _posLimit;  // offset (from _buffer) of first byte when new block reading must be done
	boolean _streamEndWasReached; // if (true) then _streamPos shows real end of stream
	
	int _pointerToLastSafePosition;
	
	public int _bufferOffset;
	
	public int _blockSize;  // Size of Allocated memory block
	public int _pos;             // offset (from _buffer) of curent byte
	int _keepSizeBefore;  // how many BYTEs must be kept in buffer before _pos
	int _keepSizeAfter;   // how many BYTEs must be kept buffer after _pos
	public int _streamPos;   // offset (from _buffer) of first not read byte from Stream
	
	public void MoveBlock()
	{
		int offset = _bufferOffset + _pos - _keepSizeBefore;
		// we need one additional byte, since MovePos moves on 1 byte.
		if (offset > 0)
			offset--;

		int numBytes = _bufferOffset + _streamPos - offset;
		
		// check negative offset ????
		for (int i = 0; i < numBytes; i++)
			_bufferBase[i] = _bufferBase[offset + i];
		_bufferOffset -= offset;
	}
	
	public void ReadBlock() throws IOException
	{
		if (_streamEndWasReached)
			return;
		while (true)
		{
			int size = (0 - _bufferOffset) + _blockSize - _streamPos;
			if (size == 0)
				return;
			int numReadBytes = _stream.read(_bufferBase, _bufferOffset + _streamPos, size);
			if (numReadBytes == -1)
			{
				_posLimit = _streamPos;
				int pointerToPostion = _bufferOffset + _posLimit;
				if (pointerToPostion > _pointerToLastSafePosition)
					_posLimit = _pointerToLastSafePosition - _bufferOffset;
				
				_streamEndWasReached = true;
				return;
			}
			_streamPos += numReadBytes;
			if (_streamPos >= _pos + _keepSizeAfter)
				_posLimit = _streamPos - _keepSizeAfter;
		}
	}
	
	void Free() { _bufferBase = null; }
	
	public void Create(int keepSizeBefore, int keepSizeAfter, int keepSizeReserv)
	{
		_keepSizeBefore = keepSizeBefore;
		_keepSizeAfter = keepSizeAfter;
		int blockSize = keepSizeBefore + keepSizeAfter + keepSizeReserv;
		if (_bufferBase == null || _blockSize != blockSize)
		{
			Free();
			_blockSize = blockSize;
			_bufferBase = new byte[_blockSize];
		}
		_pointerToLastSafePosition = _blockSize - keepSizeAfter;
	}
	
	public void SetStream(java.io.InputStream stream) { _stream = stream; 	}
	public void ReleaseStream() { _stream = null; }

	public void Init() throws IOException
	{
		_bufferOffset = 0;
		_pos = 0;
		_streamPos = 0;
		_streamEndWasReached = false;
		ReadBlock();
	}
	
	public void MovePos() throws IOException
	{
		_pos++;
		if (_pos > _posLimit)
		{
			int pointerToPostion = _bufferOffset + _pos;
			if (pointerToPostion > _pointerToLastSafePosition)
				MoveBlock();
			ReadBlock();
		}
	}
	
	public byte GetIndexByte(int index)	{ return _bufferBase[_bufferOffset + _pos + index]; }
	
	// index + limit have not to exceed _keepSizeAfter;
	public int GetMatchLen(int index, int distance, int limit)
	{
		if (_streamEndWasReached)
			if ((_pos + index) + limit > _streamPos)
				limit = _streamPos - (_pos + index);
		distance++;
		// Byte *pby = _buffer + (size_t)_pos + index;
		int pby = _bufferOffset + _pos + index;
		
		int i;
		for (i = 0; i < limit && _bufferBase[pby + i] == _bufferBase[pby + i - distance]; i++);
		return i;
	}
	
	public int GetNumAvailableBytes()	{ return _streamPos - _pos; }
	
	public void ReduceOffsets(int subValue)
	{
		_bufferOffset += subValue;
		_posLimit -= subValue;
		_pos -= subValue;
		_streamPos -= subValue;
	}
}
class OutWindow
{
	byte[] _buffer;
	int _pos;
	int _windowSize = 0;
	int _streamPos;
	java.io.OutputStream _stream;
	
	public void Create(int windowSize)
	{
		if (_buffer == null || _windowSize != windowSize)
			_buffer = new byte[windowSize];
		_windowSize = windowSize;
		_pos = 0;
		_streamPos = 0;
	}
	
	public void SetStream(java.io.OutputStream stream) throws IOException
	{
		ReleaseStream();
		_stream = stream;
	}
	
	public void ReleaseStream() throws IOException
	{
		Flush();
		_stream = null;
	}
	
	public void Init(boolean solid)
	{
		if (!solid)
		{
			_streamPos = 0;
			_pos = 0;
		}
	}
	
	public void Flush() throws IOException
	{
		int size = _pos - _streamPos;
		if (size == 0)
			return;
		_stream.write(_buffer, _streamPos, size);
		if (_pos >= _windowSize)
			_pos = 0;
		_streamPos = _pos;
	}
	
	public void CopyBlock(int distance, int len) throws IOException
	{
		int pos = _pos - distance - 1;
		if (pos < 0)
			pos += _windowSize;
		for (; len != 0; len--)
		{
			if (pos >= _windowSize)
				pos = 0;
			_buffer[_pos++] = _buffer[pos++];
			if (_pos >= _windowSize)
				Flush();
		}
	}
	
	public void PutByte(byte b) throws IOException
	{
		_buffer[_pos++] = b;
		if (_pos >= _windowSize)
			Flush();
	}
	
	public byte GetByte(int distance)
	{
		int pos = _pos - distance - 1;
		if (pos < 0)
			pos += _windowSize;
		return _buffer[pos];
	}
}
class Decoder2
{
	static final int kTopMask = ~((1 << 24) - 1);
	
	static final int kNumBitModelTotalBits = 11;
	static final int kBitModelTotal = (1 << kNumBitModelTotalBits);
	static final int kNumMoveBits = 5;
	
	int Range;
	int Code;

	java.io.InputStream Stream;
	
	public final void SetStream(java.io.InputStream stream)
	{ 
		Stream = stream; 
	}
	
	public final void ReleaseStream()
	{ 
		Stream = null; 
	}
	
	public final void Init() throws IOException
	{
		Code = 0;
		Range = -1;
		for (int i = 0; i < 5; i++)
			Code = (Code << 8) | Stream.read();
	}
	
	public final int DecodeDirectBits(int numTotalBits) throws IOException
	{
		int result = 0;
		for (int i = numTotalBits; i != 0; i--)
		{
			Range >>>= 1;
			int t = ((Code - Range) >>> 31);
			Code -= Range & (t - 1);
			result = (result << 1) | (1 - t);
			
			if ((Range & kTopMask) == 0)
			{
				Code = (Code << 8) | Stream.read();
				Range <<= 8;
			}
		}
		return result;
	}
	
	public int DecodeBit(short []probs, int index) throws IOException
	{
		int prob = probs[index];
		int newBound = (Range >>> kNumBitModelTotalBits) * prob;
		if ((Code ^ 0x80000000) < (newBound ^ 0x80000000))
		{
			Range = newBound;
			probs[index] = (short)(prob + ((kBitModelTotal - prob) >>> kNumMoveBits));
			if ((Range & kTopMask) == 0)
			{
				Code = (Code << 8) | Stream.read();
				Range <<= 8;
			}
			return 0;
		}
		else
		{
			Range -= newBound;
			Code -= newBound;
			probs[index] = (short)(prob - ((prob) >>> kNumMoveBits));
			if ((Range & kTopMask) == 0)
			{
				Code = (Code << 8) | Stream.read();
				Range <<= 8;
			}
			return 1;
		}
	}
	
	public static void InitBitModels(short []probs)
	{
		for (int i = 0; i < probs.length; i++)
			probs[i] = (kBitModelTotal >>> 1);
	}
}
class Encoder2
{
	static final int kTopMask = ~((1 << 24) - 1);
	
	static final int kNumBitModelTotalBits = 11;
	static final int kBitModelTotal = (1 << kNumBitModelTotalBits);
	static final int kNumMoveBits = 5;
	
	java.io.OutputStream Stream;

	long Low;
	int Range;
	int _cacheSize;
	int _cache;
	
	long _position;
	
	public void SetStream(java.io.OutputStream stream)
	{
		Stream = stream;
	}
	
	public void ReleaseStream()
	{
		Stream = null;
	}
	
	public void Init()
	{
		_position = 0;
		Low = 0;
		Range = -1;
		_cacheSize = 1;
		_cache = 0;
	}
	
	public void FlushData() throws IOException
	{
		for (int i = 0; i < 5; i++)
			ShiftLow();
	}
	
	public void FlushStream() throws IOException
	{
		Stream.flush();
	}
	
	public void ShiftLow() throws IOException
	{
		int LowHi = (int)(Low >>> 32);
		if (LowHi != 0 || Low < 0xFF000000L)
		{
			_position += _cacheSize;
			int temp = _cache;
			do
			{
				Stream.write(temp + LowHi);
				temp = 0xFF;
			}
			while(--_cacheSize != 0);
			_cache = (((int)Low) >>> 24);
		}
		_cacheSize++;
		Low = (Low & 0xFFFFFF) << 8;
	}
	
	public void EncodeDirectBits(int v, int numTotalBits) throws IOException
	{
		for (int i = numTotalBits - 1; i >= 0; i--)
		{
			Range >>>= 1;
			if (((v >>> i) & 1) == 1)
				Low += Range;
			if ((Range & Encoder2.kTopMask) == 0)
			{
				Range <<= 8;
				ShiftLow();
			}
		}
	}
	
	
	public long GetProcessedSizeAdd()
	{
		return _cacheSize + _position + 4;
	}
	
	
	
	static final int kNumMoveReducingBits = 2;
	public static final int kNumBitPriceShiftBits = 6;
	
	public static void InitBitModels(short []probs)
	{
		for (int i = 0; i < probs.length; i++)
			probs[i] = (kBitModelTotal >>> 1);
	}
	
	public void Encode(short []probs, int index, int symbol) throws IOException
	{
		int prob = probs[index];
		int newBound = (Range >>> kNumBitModelTotalBits) * prob;
		if (symbol == 0)
		{
			Range = newBound;
			probs[index] = (short)(prob + ((kBitModelTotal - prob) >>> kNumMoveBits));
		}
		else
		{
			Low += (newBound & 0xFFFFFFFFL);
			Range -= newBound;
			probs[index] = (short)(prob - ((prob) >>> kNumMoveBits));
		}
		if ((Range & kTopMask) == 0)
		{
			Range <<= 8;
			ShiftLow();
		}
	}
	
	private static int[] ProbPrices = new int[kBitModelTotal >>> kNumMoveReducingBits];
	
	static
	{
		int kNumBits = (kNumBitModelTotalBits - kNumMoveReducingBits);
		for (int i = kNumBits - 1; i >= 0; i--)
		{
			int start = 1 << (kNumBits - i - 1);
			int end = 1 << (kNumBits - i);
			for (int j = start; j < end; j++)
				ProbPrices[j] = (i << kNumBitPriceShiftBits) +
						(((end - j) << kNumBitPriceShiftBits) >>> (kNumBits - i - 1));
		}
	}
	
	static public int GetPrice(int Prob, int symbol)
	{
		return ProbPrices[(((Prob - symbol) ^ ((-symbol))) & (kBitModelTotal - 1)) >>> kNumMoveReducingBits];
	}
	static public int GetPrice0(int Prob)
	{ 
		return ProbPrices[Prob >>> kNumMoveReducingBits]; 
	}
	static public int GetPrice1(int Prob)
	{ 
		return ProbPrices[(kBitModelTotal - Prob) >>> kNumMoveReducingBits]; 
	}
}

class CRC
{
	static public int[] Table = new int[256];
	
	static
	{
		for (int i = 0; i < 256; i++)
		{
			int r = i;
			for (int j = 0; j < 8; j++)
				if ((r & 1) != 0)
					r = (r >>> 1) ^ 0xEDB88320;
				else
					r >>>= 1;
			Table[i] = r;
		}
	}
	
	int _value = -1;
	
	public void Init()
	{
		_value = -1;
	}
	
	public void Update(byte[] data, int offset, int size)
	{
		for (int i = 0; i < size; i++)
			_value = Table[(_value ^ data[offset + i]) & 0xFF] ^ (_value >>> 8);
	}
	
	public void Update(byte[] data)
	{
		int size = data.length;
		for (int i = 0; i < size; i++)
			_value = Table[(_value ^ data[i]) & 0xFF] ^ (_value >>> 8);
	}
	
	public void UpdateByte(int b)
	{
		_value = Table[(_value ^ b) & 0xFF] ^ (_value >>> 8);
	}
	
	public int GetDigest()
	{
		return _value ^ (-1);
	}
}
class Decoder
{
	class LenDecoder
	{
		short[] m_Choice = new short[2];
		BitTreeDecoder[] m_LowCoder = new BitTreeDecoder[Base.kNumPosStatesMax];
		BitTreeDecoder[] m_MidCoder = new BitTreeDecoder[Base.kNumPosStatesMax];
		BitTreeDecoder m_HighCoder = new BitTreeDecoder(Base.kNumHighLenBits);
		int m_NumPosStates = 0;
		
		public void Create(int numPosStates)
		{
			for (; m_NumPosStates < numPosStates; m_NumPosStates++)
			{
				m_LowCoder[m_NumPosStates] = new BitTreeDecoder(Base.kNumLowLenBits);
				m_MidCoder[m_NumPosStates] = new BitTreeDecoder(Base.kNumMidLenBits);
			}
		}
		
		public void Init()
		{
			chapter03.exercises.Decoder2.InitBitModels(m_Choice);
			for (int posState = 0; posState < m_NumPosStates; posState++)
			{
				m_LowCoder[posState].Init();
				m_MidCoder[posState].Init();
			}
			m_HighCoder.Init();
		}
		
		public int Decode(chapter03.exercises.Decoder2 rangeDecoder, int posState) throws IOException
		{
			if (rangeDecoder.DecodeBit(m_Choice, 0) == 0)
				return m_LowCoder[posState].Decode(rangeDecoder);
			int symbol = Base.kNumLowLenSymbols;
			if (rangeDecoder.DecodeBit(m_Choice, 1) == 0)
				symbol += m_MidCoder[posState].Decode(rangeDecoder);
			else
				symbol += Base.kNumMidLenSymbols + m_HighCoder.Decode(rangeDecoder);
			return symbol;
		}
	}
	
	class LiteralDecoder
	{
		class Decoder2
		{
			short[] m_Decoders = new short[0x300];
			
			public void Init()
			{
				chapter03.exercises.Decoder2.InitBitModels(m_Decoders);
			}
			
			public byte DecodeNormal(chapter03.exercises.Decoder2 rangeDecoder) throws IOException
			{
				int symbol = 1;
				do
					symbol = (symbol << 1) | rangeDecoder.DecodeBit(m_Decoders, symbol);
				while (symbol < 0x100);
				return (byte)symbol;
			}
			
			public byte DecodeWithMatchByte(chapter03.exercises.Decoder2 rangeDecoder, byte matchByte) throws IOException
			{
				int symbol = 1;
				do
				{
					int matchBit = (matchByte >> 7) & 1;
					matchByte <<= 1;
					int bit = rangeDecoder.DecodeBit(m_Decoders, ((1 + matchBit) << 8) + symbol);
					symbol = (symbol << 1) | bit;
					if (matchBit != bit)
					{
						while (symbol < 0x100)
							symbol = (symbol << 1) | rangeDecoder.DecodeBit(m_Decoders, symbol);
						break;
					}
				}
				while (symbol < 0x100);
				return (byte)symbol;
			}
		}
		
		Decoder2[] m_Coders;
		int m_NumPrevBits;
		int m_NumPosBits;
		int m_PosMask;
		
		public void Create(int numPosBits, int numPrevBits)
		{
			if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits)
				return;
			m_NumPosBits = numPosBits;
			m_PosMask = (1 << numPosBits) - 1;
			m_NumPrevBits = numPrevBits;
			int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
			m_Coders = new Decoder2[numStates];
			for (int i = 0; i < numStates; i++)
				m_Coders[i] = new Decoder2();
		}
		
		public void Init()
		{
			int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
			for (int i = 0; i < numStates; i++)
				m_Coders[i].Init();
		}
		
		Decoder2 GetDecoder(int pos, byte prevByte)
		{
			return m_Coders[((pos & m_PosMask) << m_NumPrevBits) + ((prevByte & 0xFF) >>> (8 - m_NumPrevBits))];
		}
	}
	
	OutWindow m_OutWindow = new OutWindow();
	chapter03.exercises.Decoder2 m_RangeDecoder = new chapter03.exercises.Decoder2();
	
	short[] m_IsMatchDecoders = new short[Base.kNumStates << Base.kNumPosStatesBitsMax];
	short[] m_IsRepDecoders = new short[Base.kNumStates];
	short[] m_IsRepG0Decoders = new short[Base.kNumStates];
	short[] m_IsRepG1Decoders = new short[Base.kNumStates];
	short[] m_IsRepG2Decoders = new short[Base.kNumStates];
	short[] m_IsRep0LongDecoders = new short[Base.kNumStates << Base.kNumPosStatesBitsMax];
	
	BitTreeDecoder[] m_PosSlotDecoder = new BitTreeDecoder[Base.kNumLenToPosStates];
	short[] m_PosDecoders = new short[Base.kNumFullDistances - Base.kEndPosModelIndex];
	
	BitTreeDecoder m_PosAlignDecoder = new BitTreeDecoder(Base.kNumAlignBits);
	
	LenDecoder m_LenDecoder = new LenDecoder();
	LenDecoder m_RepLenDecoder = new LenDecoder();
	
	LiteralDecoder m_LiteralDecoder = new LiteralDecoder();
	
	int m_DictionarySize = -1;
	int m_DictionarySizeCheck =  -1;
	
	int m_PosStateMask;
	
	public Decoder()
	{
		for (int i = 0; i < Base.kNumLenToPosStates; i++)
			m_PosSlotDecoder[i] = new BitTreeDecoder(Base.kNumPosSlotBits);
	}
	
	boolean SetDictionarySize(int dictionarySize)
	{
		if (dictionarySize < 0)
			return false;
		if (m_DictionarySize != dictionarySize)
		{
			m_DictionarySize = dictionarySize;
			m_DictionarySizeCheck = Math.max(m_DictionarySize, 1);
			m_OutWindow.Create(Math.max(m_DictionarySizeCheck, (1 << 12)));
		}
		return true;
	}
	
	boolean SetLcLpPb(int lc, int lp, int pb)
	{
		if (lc > Base.kNumLitContextBitsMax || lp > 4 || pb > Base.kNumPosStatesBitsMax)
			return false;
		m_LiteralDecoder.Create(lp, lc);
		int numPosStates = 1 << pb;
		m_LenDecoder.Create(numPosStates);
		m_RepLenDecoder.Create(numPosStates);
		m_PosStateMask = numPosStates - 1;
		return true;
	}
	
	void Init() throws IOException
	{
		m_OutWindow.Init(false);
		
		chapter03.exercises.Decoder2.InitBitModels(m_IsMatchDecoders);
		chapter03.exercises.Decoder2.InitBitModels(m_IsRep0LongDecoders);
		chapter03.exercises.Decoder2.InitBitModels(m_IsRepDecoders);
		chapter03.exercises.Decoder2.InitBitModels(m_IsRepG0Decoders);
		chapter03.exercises.Decoder2.InitBitModels(m_IsRepG1Decoders);
		chapter03.exercises.Decoder2.InitBitModels(m_IsRepG2Decoders);
		chapter03.exercises.Decoder2.InitBitModels(m_PosDecoders);
		
		m_LiteralDecoder.Init();
		int i;
		for (i = 0; i < Base.kNumLenToPosStates; i++)
			m_PosSlotDecoder[i].Init();
		m_LenDecoder.Init();
		m_RepLenDecoder.Init();
		m_PosAlignDecoder.Init();
		m_RangeDecoder.Init();
	}
	
	public boolean Code(java.io.InputStream inStream, java.io.OutputStream outStream,
			long outSize) throws IOException
	{
		m_RangeDecoder.SetStream(inStream);
		m_OutWindow.SetStream(outStream);
		Init();
		
		int state = Base.StateInit();
		int rep0 = 0, rep1 = 0, rep2 = 0, rep3 = 0;
		
		long nowPos64 = 0;
		byte prevByte = 0;
		while (outSize < 0 || nowPos64 < outSize)
		{
			int posState = (int)nowPos64 & m_PosStateMask;
			if (m_RangeDecoder.DecodeBit(m_IsMatchDecoders, (state << Base.kNumPosStatesBitsMax) + posState) == 0)
			{
				LiteralDecoder.Decoder2 decoder2 = m_LiteralDecoder.GetDecoder((int)nowPos64, prevByte);
				if (!Base.StateIsCharState(state))
					prevByte = decoder2.DecodeWithMatchByte(m_RangeDecoder, m_OutWindow.GetByte(rep0));
				else
					prevByte = decoder2.DecodeNormal(m_RangeDecoder);
				m_OutWindow.PutByte(prevByte);
				state = Base.StateUpdateChar(state);
				nowPos64++;
			}
			else
			{
				int len;
				if (m_RangeDecoder.DecodeBit(m_IsRepDecoders, state) == 1)
				{
					len = 0;
					if (m_RangeDecoder.DecodeBit(m_IsRepG0Decoders, state) == 0)
					{
						if (m_RangeDecoder.DecodeBit(m_IsRep0LongDecoders, (state << Base.kNumPosStatesBitsMax) + posState) == 0)
						{
							state = Base.StateUpdateShortRep(state);
							len = 1;
						}
					}
					else
					{
						int distance;
						if (m_RangeDecoder.DecodeBit(m_IsRepG1Decoders, state) == 0)
							distance = rep1;
						else
						{
							if (m_RangeDecoder.DecodeBit(m_IsRepG2Decoders, state) == 0)
								distance = rep2;
							else
							{
								distance = rep3;
								rep3 = rep2;
							}
							rep2 = rep1;
						}
						rep1 = rep0;
						rep0 = distance;
					}
					if (len == 0)
					{
						len = m_RepLenDecoder.Decode(m_RangeDecoder, posState) + Base.kMatchMinLen;
						state = Base.StateUpdateRep(state);
					}
				}
				else
				{
					rep3 = rep2;
					rep2 = rep1;
					rep1 = rep0;
					len = Base.kMatchMinLen + m_LenDecoder.Decode(m_RangeDecoder, posState);
					state = Base.StateUpdateMatch(state);
					int posSlot = m_PosSlotDecoder[Base.GetLenToPosState(len)].Decode(m_RangeDecoder);
					if (posSlot >= Base.kStartPosModelIndex)
					{
						int numDirectBits = (posSlot >> 1) - 1;
						rep0 = ((2 | (posSlot & 1)) << numDirectBits);
						if (posSlot < Base.kEndPosModelIndex)
							rep0 += BitTreeDecoder.ReverseDecode(m_PosDecoders,
									rep0 - posSlot - 1, m_RangeDecoder, numDirectBits);
						else
						{
							rep0 += (m_RangeDecoder.DecodeDirectBits(
									numDirectBits - Base.kNumAlignBits) << Base.kNumAlignBits);
							rep0 += m_PosAlignDecoder.ReverseDecode(m_RangeDecoder);
							if (rep0 < 0)
							{
								if (rep0 == -1)
									break;
								return false;
							}
						}
					}
					else
						rep0 = posSlot;
				}
				if (rep0 >= nowPos64 || rep0 >= m_DictionarySizeCheck)
				{
					// m_OutWindow.Flush();
					return false;
				}
				m_OutWindow.CopyBlock(rep0, len);
				nowPos64 += len;
				prevByte = m_OutWindow.GetByte(0);
			}
		}
		m_OutWindow.Flush();
		m_OutWindow.ReleaseStream();
		m_RangeDecoder.ReleaseStream();
		return true;
	}
	
	public boolean SetDecoderProperties(byte[] properties)
	{
		if (properties.length < 5)
			return false;
		int val = properties[0] & 0xFF;
		int lc = val % 9;
		int remainder = val / 9;
		int lp = remainder % 5;
		int pb = remainder / 5;
		int dictionarySize = 0;
		for (int i = 0; i < 4; i++)
			dictionarySize += ((int)(properties[1 + i]) & 0xFF) << (i * 8);
		if (!SetLcLpPb(lc, lp, pb))
			return false;
		return SetDictionarySize(dictionarySize);
	}
}
class Encoder
{
	public static final int EMatchFinderTypeBT2 = 0;
	public static final int EMatchFinderTypeBT4 = 1;

	static final int kIfinityPrice = 0xFFFFFFF;

	static byte[] g_FastPos = new byte[1 << 11];

	static
	{
		int kFastSlots = 22;
		int c = 2;
		g_FastPos[0] = 0;
		g_FastPos[1] = 1;
		for (int slotFast = 2; slotFast < kFastSlots; slotFast++)
		{
			int k = (1 << ((slotFast >> 1) - 1));
			for (int j = 0; j < k; j++, c++)
				g_FastPos[c] = (byte)slotFast;
		}
	}

	static int GetPosSlot(int pos)
	{
		if (pos < (1 << 11))
			return g_FastPos[pos];
		if (pos < (1 << 21))
			return (g_FastPos[pos >> 10] + 20);
		return (g_FastPos[pos >> 20] + 40);
	}

	static int GetPosSlot2(int pos)
	{
		if (pos < (1 << 17))
			return (g_FastPos[pos >> 6] + 12);
		if (pos < (1 << 27))
			return (g_FastPos[pos >> 16] + 32);
		return (g_FastPos[pos >> 26] + 52);
	}

	int _state = Base.StateInit();
	byte _previousByte;
	int[] _repDistances = new int[Base.kNumRepDistances];

	void BaseInit()
	{
		_state = Base.StateInit();
		_previousByte = 0;
		for (int i = 0; i < Base.kNumRepDistances; i++)
			_repDistances[i] = 0;
	}

	static final int kDefaultDictionaryLogSize = 22;
	static final int kNumFastBytesDefault = 0x20;

	class LiteralEncoder
	{
		class Encoder2
		{
			short[] m_Encoders = new short[0x300];

			public void Init() { chapter03.exercises.Encoder2.InitBitModels(m_Encoders); }



			public void Encode(chapter03.exercises.Encoder2 rangeEncoder, byte symbol) throws IOException
			{
				int context = 1;
				for (int i = 7; i >= 0; i--)
				{
					int bit = ((symbol >> i) & 1);
					rangeEncoder.Encode(m_Encoders, context, bit);
					context = (context << 1) | bit;
				}
			}

			public void EncodeMatched(chapter03.exercises.Encoder2 rangeEncoder, byte matchByte, byte symbol) throws IOException
			{
				int context = 1;
				boolean same = true;
				for (int i = 7; i >= 0; i--)
				{
					int bit = ((symbol >> i) & 1);
					int state = context;
					if (same)
					{
						int matchBit = ((matchByte >> i) & 1);
						state += ((1 + matchBit) << 8);
						same = (matchBit == bit);
					}
					rangeEncoder.Encode(m_Encoders, state, bit);
					context = (context << 1) | bit;
				}
			}

			public int GetPrice(boolean matchMode, byte matchByte, byte symbol)
			{
				int price = 0;
				int context = 1;
				int i = 7;
				if (matchMode)
				{
					for (; i >= 0; i--)
					{
						int matchBit = (matchByte >> i) & 1;
						int bit = (symbol >> i) & 1;
						price += chapter03.exercises.Encoder2.GetPrice(m_Encoders[((1 + matchBit) << 8) + context], bit);
						context = (context << 1) | bit;
						if (matchBit != bit)
						{
							i--;
							break;
						}
					}
				}
				for (; i >= 0; i--)
				{
					int bit = (symbol >> i) & 1;
					price += chapter03.exercises.Encoder2.GetPrice(m_Encoders[context], bit);
					context = (context << 1) | bit;
				}
				return price;
			}
		}

		Encoder2[] m_Coders;
		int m_NumPrevBits;
		int m_NumPosBits;
		int m_PosMask;

		public void Create(int numPosBits, int numPrevBits)
		{
			if (m_Coders != null && m_NumPrevBits == numPrevBits && m_NumPosBits == numPosBits)
				return;
			m_NumPosBits = numPosBits;
			m_PosMask = (1 << numPosBits) - 1;
			m_NumPrevBits = numPrevBits;
			int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
			m_Coders = new Encoder2[numStates];
			for (int i = 0; i < numStates; i++)
				m_Coders[i] = new Encoder2();
		}

		public void Init()
		{
			int numStates = 1 << (m_NumPrevBits + m_NumPosBits);
			for (int i = 0; i < numStates; i++)
				m_Coders[i].Init();
		}

		public Encoder2 GetSubCoder(int pos, byte prevByte)
		{ return m_Coders[((pos & m_PosMask) << m_NumPrevBits) + ((prevByte & 0xFF) >>> (8 - m_NumPrevBits))]; }
	}

	class LenEncoder
	{
		short[] _choice = new short[2];
		BitTreeEncoder[] _lowCoder = new BitTreeEncoder[Base.kNumPosStatesEncodingMax];
		BitTreeEncoder[] _midCoder = new BitTreeEncoder[Base.kNumPosStatesEncodingMax];
		BitTreeEncoder _highCoder = new BitTreeEncoder(Base.kNumHighLenBits);


		public LenEncoder()
		{
			for (int posState = 0; posState < Base.kNumPosStatesEncodingMax; posState++)
			{
				_lowCoder[posState] = new BitTreeEncoder(Base.kNumLowLenBits);
				_midCoder[posState] = new BitTreeEncoder(Base.kNumMidLenBits);
			}
		}

		public void Init(int numPosStates)
		{
			chapter03.exercises.Encoder2.InitBitModels(_choice);

			for (int posState = 0; posState < numPosStates; posState++)
			{
				_lowCoder[posState].Init();
				_midCoder[posState].Init();
			}
			_highCoder.Init();
		}

		public void Encode(chapter03.exercises.Encoder2 rangeEncoder, int symbol, int posState) throws IOException
		{
			if (symbol < Base.kNumLowLenSymbols)
			{
				rangeEncoder.Encode(_choice, 0, 0);
				_lowCoder[posState].Encode(rangeEncoder, symbol);
			}
			else
			{
				symbol -= Base.kNumLowLenSymbols;
				rangeEncoder.Encode(_choice, 0, 1);
				if (symbol < Base.kNumMidLenSymbols)
				{
					rangeEncoder.Encode(_choice, 1, 0);
					_midCoder[posState].Encode(rangeEncoder, symbol);
				}
				else
				{
					rangeEncoder.Encode(_choice, 1, 1);
					_highCoder.Encode(rangeEncoder, symbol - Base.kNumMidLenSymbols);
				}
			}
		}

		public void SetPrices(int posState, int numSymbols, int[] prices, int st)
		{
			int a0 = chapter03.exercises.Encoder2.GetPrice0(_choice[0]);
			int a1 = chapter03.exercises.Encoder2.GetPrice1(_choice[0]);
			int b0 = a1 + chapter03.exercises.Encoder2.GetPrice0(_choice[1]);
			int b1 = a1 + chapter03.exercises.Encoder2.GetPrice1(_choice[1]);
			int i = 0;
			for (i = 0; i < Base.kNumLowLenSymbols; i++)
			{
				if (i >= numSymbols)
					return;
				prices[st + i] = a0 + _lowCoder[posState].GetPrice(i);
			}
			for (; i < Base.kNumLowLenSymbols + Base.kNumMidLenSymbols; i++)
			{
				if (i >= numSymbols)
					return;
				prices[st + i] = b0 + _midCoder[posState].GetPrice(i - Base.kNumLowLenSymbols);
			}
			for (; i < numSymbols; i++)
				prices[st + i] = b1 + _highCoder.GetPrice(i - Base.kNumLowLenSymbols - Base.kNumMidLenSymbols);
		}
	};

	public static final int kNumLenSpecSymbols = Base.kNumLowLenSymbols + Base.kNumMidLenSymbols;

	class LenPriceTableEncoder extends LenEncoder
	{
		int[] _prices = new int[Base.kNumLenSymbols<<Base.kNumPosStatesBitsEncodingMax];
		int _tableSize;
		int[] _counters = new int[Base.kNumPosStatesEncodingMax];

		public void SetTableSize(int tableSize) { _tableSize = tableSize; }

		public int GetPrice(int symbol, int posState)
		{
			return _prices[posState * Base.kNumLenSymbols + symbol];
		}

		void UpdateTable(int posState)
		{
			SetPrices(posState, _tableSize, _prices, posState * Base.kNumLenSymbols);
			_counters[posState] = _tableSize;
		}

		public void UpdateTables(int numPosStates)
		{
			for (int posState = 0; posState < numPosStates; posState++)
				UpdateTable(posState);
		}

		public void Encode(chapter03.exercises.Encoder2 rangeEncoder, int symbol, int posState) throws IOException
		{
			super.Encode(rangeEncoder, symbol, posState);
			if (--_counters[posState] == 0)
				UpdateTable(posState);
		}
	}

	static final int kNumOpts = 1 << 12;
	class Optimal
	{
		public int State;

		public boolean Prev1IsChar;
		public boolean Prev2;

		public int PosPrev2;
		public int BackPrev2;

		public int Price;
		public int PosPrev;
		public int BackPrev;

		public int Backs0;
		public int Backs1;
		public int Backs2;
		public int Backs3;

		public void MakeAsChar() { BackPrev = -1; Prev1IsChar = false; }
		public void MakeAsShortRep() { BackPrev = 0; ; Prev1IsChar = false; }
		public boolean IsShortRep() { return (BackPrev == 0); }
	};
	Optimal[] _optimum = new Optimal[kNumOpts];
	chapter03.exercises.BinTree _matchFinder = null;
	chapter03.exercises.Encoder2 _rangeEncoder = new chapter03.exercises.Encoder2();

	short[] _isMatch = new short[Base.kNumStates<<Base.kNumPosStatesBitsMax];
	short[] _isRep = new short[Base.kNumStates];
	short[] _isRepG0 = new short[Base.kNumStates];
	short[] _isRepG1 = new short[Base.kNumStates];
	short[] _isRepG2 = new short[Base.kNumStates];
	short[] _isRep0Long = new short[Base.kNumStates<<Base.kNumPosStatesBitsMax];

	BitTreeEncoder[] _posSlotEncoder = new BitTreeEncoder[Base.kNumLenToPosStates]; // kNumPosSlotBits

	short[] _posEncoders = new short[Base.kNumFullDistances-Base.kEndPosModelIndex];
	BitTreeEncoder _posAlignEncoder = new BitTreeEncoder(Base.kNumAlignBits);

	LenPriceTableEncoder _lenEncoder = new LenPriceTableEncoder();
	LenPriceTableEncoder _repMatchLenEncoder = new LenPriceTableEncoder();

	LiteralEncoder _literalEncoder = new LiteralEncoder();

	int[] _matchDistances = new int[Base.kMatchMaxLen*2+2];

	int _numFastBytes = kNumFastBytesDefault;
	int _longestMatchLength;
	int _numDistancePairs;

	int _additionalOffset;

	int _optimumEndIndex;
	int _optimumCurrentIndex;

	boolean _longestMatchWasFound;

	int[] _posSlotPrices = new int[1<<(Base.kNumPosSlotBits+Base.kNumLenToPosStatesBits)];
	int[] _distancesPrices = new int[Base.kNumFullDistances<<Base.kNumLenToPosStatesBits];
	int[] _alignPrices = new int[Base.kAlignTableSize];
	int _alignPriceCount;

	int _distTableSize = (kDefaultDictionaryLogSize * 2);

	int _posStateBits = 2;
	int _posStateMask = (4 - 1);
	int _numLiteralPosStateBits = 0;
	int _numLiteralContextBits = 3;

	int _dictionarySize = (1 << kDefaultDictionaryLogSize);
	int _dictionarySizePrev = -1;
	int _numFastBytesPrev = -1;

	long nowPos64;
	boolean _finished;
	java.io.InputStream _inStream;

	int _matchFinderType = EMatchFinderTypeBT4;
	boolean _writeEndMark = false;

	boolean _needReleaseMFStream = false;

	void Create()
	{
		if (_matchFinder == null)
		{
			chapter03.exercises.BinTree bt = new chapter03.exercises.BinTree();
			int numHashBytes = 4;
			if (_matchFinderType == EMatchFinderTypeBT2)
				numHashBytes = 2;
			bt.SetType(numHashBytes);
			_matchFinder = bt;
		}
		_literalEncoder.Create(_numLiteralPosStateBits, _numLiteralContextBits);

		if (_dictionarySize == _dictionarySizePrev && _numFastBytesPrev == _numFastBytes)
			return;
		_matchFinder.Create(_dictionarySize, kNumOpts, _numFastBytes, Base.kMatchMaxLen + 1);
		_dictionarySizePrev = _dictionarySize;
		_numFastBytesPrev = _numFastBytes;
	}

	public Encoder()
	{
		for (int i = 0; i < kNumOpts; i++)
			_optimum[i] = new Optimal();
		for (int i = 0; i < Base.kNumLenToPosStates; i++)
			_posSlotEncoder[i] = new BitTreeEncoder(Base.kNumPosSlotBits);
	}

	void SetWriteEndMarkerMode(boolean writeEndMarker)
	{
		_writeEndMark = writeEndMarker;
	}

	void Init()
	{
		BaseInit();
		_rangeEncoder.Init();

		chapter03.exercises.Encoder2.InitBitModels(_isMatch);
		chapter03.exercises.Encoder2.InitBitModels(_isRep0Long);
		chapter03.exercises.Encoder2.InitBitModels(_isRep);
		chapter03.exercises.Encoder2.InitBitModels(_isRepG0);
		chapter03.exercises.Encoder2.InitBitModels(_isRepG1);
		chapter03.exercises.Encoder2.InitBitModels(_isRepG2);
		chapter03.exercises.Encoder2.InitBitModels(_posEncoders);







		_literalEncoder.Init();
		for (int i = 0; i < Base.kNumLenToPosStates; i++)
			_posSlotEncoder[i].Init();



		_lenEncoder.Init(1 << _posStateBits);
		_repMatchLenEncoder.Init(1 << _posStateBits);

		_posAlignEncoder.Init();

		_longestMatchWasFound = false;
		_optimumEndIndex = 0;
		_optimumCurrentIndex = 0;
		_additionalOffset = 0;
	}

	int ReadMatchDistances() throws java.io.IOException
	{
		int lenRes = 0;
		_numDistancePairs = _matchFinder.GetMatches(_matchDistances);
		if (_numDistancePairs > 0)
		{
			lenRes = _matchDistances[_numDistancePairs - 2];
			if (lenRes == _numFastBytes)
				lenRes += _matchFinder.GetMatchLen((int)lenRes - 1, _matchDistances[_numDistancePairs - 1],
					Base.kMatchMaxLen - lenRes);
		}
		_additionalOffset++;
		return lenRes;
	}

	void MovePos(int num) throws java.io.IOException
	{
		if (num > 0)
		{
			_matchFinder.Skip(num);
			_additionalOffset += num;
		}
	}

	int GetRepLen1Price(int state, int posState)
	{
		return chapter03.exercises.Encoder2.GetPrice0(_isRepG0[state]) +
				chapter03.exercises.Encoder2.GetPrice0(_isRep0Long[(state << Base.kNumPosStatesBitsMax) + posState]);
	}

	int GetPureRepPrice(int repIndex, int state, int posState)
	{
		int price;
		if (repIndex == 0)
		{
			price = chapter03.exercises.Encoder2.GetPrice0(_isRepG0[state]);
			price += chapter03.exercises.Encoder2.GetPrice1(_isRep0Long[(state << Base.kNumPosStatesBitsMax) + posState]);
		}
		else
		{
			price = chapter03.exercises.Encoder2.GetPrice1(_isRepG0[state]);
			if (repIndex == 1)
				price += chapter03.exercises.Encoder2.GetPrice0(_isRepG1[state]);
			else
			{
				price += chapter03.exercises.Encoder2.GetPrice1(_isRepG1[state]);
				price += chapter03.exercises.Encoder2.GetPrice(_isRepG2[state], repIndex - 2);
			}
		}
		return price;
	}

	int GetRepPrice(int repIndex, int len, int state, int posState)
	{
		int price = _repMatchLenEncoder.GetPrice(len - Base.kMatchMinLen, posState);
		return price + GetPureRepPrice(repIndex, state, posState);
	}

	int GetPosLenPrice(int pos, int len, int posState)
	{
		int price;
		int lenToPosState = Base.GetLenToPosState(len);
		if (pos < Base.kNumFullDistances)
			price = _distancesPrices[(lenToPosState * Base.kNumFullDistances) + pos];
		else
			price = _posSlotPrices[(lenToPosState << Base.kNumPosSlotBits) + GetPosSlot2(pos)] +
				_alignPrices[pos & Base.kAlignMask];
		return price + _lenEncoder.GetPrice(len - Base.kMatchMinLen, posState);
	}

	int Backward(int cur)
	{
		_optimumEndIndex = cur;
		int posMem = _optimum[cur].PosPrev;
		int backMem = _optimum[cur].BackPrev;
		do
		{
			if (_optimum[cur].Prev1IsChar)
			{
				_optimum[posMem].MakeAsChar();
				_optimum[posMem].PosPrev = posMem - 1;
				if (_optimum[cur].Prev2)
				{
					_optimum[posMem - 1].Prev1IsChar = false;
					_optimum[posMem - 1].PosPrev = _optimum[cur].PosPrev2;
					_optimum[posMem - 1].BackPrev = _optimum[cur].BackPrev2;
				}
			}
			int posPrev = posMem;
			int backCur = backMem;

			backMem = _optimum[posPrev].BackPrev;
			posMem = _optimum[posPrev].PosPrev;

			_optimum[posPrev].BackPrev = backCur;
			_optimum[posPrev].PosPrev = cur;
			cur = posPrev;
		}
		while (cur > 0);
		backRes = _optimum[0].BackPrev;
		_optimumCurrentIndex = _optimum[0].PosPrev;
		return _optimumCurrentIndex;
	}

	int[] reps = new int[Base.kNumRepDistances];
	int[] repLens = new int[Base.kNumRepDistances];
	int backRes;

	int GetOptimum(int position) throws IOException
	{
		if (_optimumEndIndex != _optimumCurrentIndex)
		{
			int lenRes = _optimum[_optimumCurrentIndex].PosPrev - _optimumCurrentIndex;
			backRes = _optimum[_optimumCurrentIndex].BackPrev;
			_optimumCurrentIndex = _optimum[_optimumCurrentIndex].PosPrev;
			return lenRes;
		}
		_optimumCurrentIndex = _optimumEndIndex = 0;

		int lenMain, numDistancePairs;
		if (!_longestMatchWasFound)
		{
			lenMain = ReadMatchDistances();
		}
		else
		{
			lenMain = _longestMatchLength;
			_longestMatchWasFound = false;
		}
		numDistancePairs = _numDistancePairs;

		int numAvailableBytes = _matchFinder.GetNumAvailableBytes() + 1;
		if (numAvailableBytes < 2)
		{
			backRes = -1;
			return 1;
		}
		if (numAvailableBytes > Base.kMatchMaxLen)
			numAvailableBytes = Base.kMatchMaxLen;

		int repMaxIndex = 0;
		int i;
		for (i = 0; i < Base.kNumRepDistances; i++)
		{
			reps[i] = _repDistances[i];
			repLens[i] = _matchFinder.GetMatchLen(0 - 1, reps[i], Base.kMatchMaxLen);
			if (repLens[i] > repLens[repMaxIndex])
				repMaxIndex = i;
		}
		if (repLens[repMaxIndex] >= _numFastBytes)
		{
			backRes = repMaxIndex;
			int lenRes = repLens[repMaxIndex];
			MovePos(lenRes - 1);
			return lenRes;
		}

		if (lenMain >= _numFastBytes)
		{
			backRes = _matchDistances[numDistancePairs - 1] + Base.kNumRepDistances;
			MovePos(lenMain - 1);
			return lenMain;
		}

		byte currentByte = _matchFinder.GetIndexByte(0 - 1);
		byte matchByte = _matchFinder.GetIndexByte(0 - _repDistances[0] - 1 - 1);

		if (lenMain < 2 && currentByte != matchByte && repLens[repMaxIndex] < 2)
		{
			backRes = -1;
			return 1;
		}

		_optimum[0].State = _state;

		int posState = (position & _posStateMask);

		_optimum[1].Price = chapter03.exercises.Encoder2.GetPrice0(_isMatch[(_state << Base.kNumPosStatesBitsMax) + posState]) +
				_literalEncoder.GetSubCoder(position, _previousByte).GetPrice(!Base.StateIsCharState(_state), matchByte, currentByte);
		_optimum[1].MakeAsChar();

		int matchPrice = chapter03.exercises.Encoder2.GetPrice1(_isMatch[(_state << Base.kNumPosStatesBitsMax) + posState]);
		int repMatchPrice = matchPrice + chapter03.exercises.Encoder2.GetPrice1(_isRep[_state]);

		if (matchByte == currentByte)
		{
			int shortRepPrice = repMatchPrice + GetRepLen1Price(_state, posState);
			if (shortRepPrice < _optimum[1].Price)
			{
				_optimum[1].Price = shortRepPrice;
				_optimum[1].MakeAsShortRep();
			}
		}

		int lenEnd = ((lenMain >= repLens[repMaxIndex]) ? lenMain : repLens[repMaxIndex]);

		if (lenEnd < 2)
		{
			backRes = _optimum[1].BackPrev;
			return 1;
		}

		_optimum[1].PosPrev = 0;

		_optimum[0].Backs0 = reps[0];
		_optimum[0].Backs1 = reps[1];
		_optimum[0].Backs2 = reps[2];
		_optimum[0].Backs3 = reps[3];

		int len = lenEnd;
		do
			_optimum[len--].Price = kIfinityPrice;
		while (len >= 2);

		for (i = 0; i < Base.kNumRepDistances; i++)
		{
			int repLen = repLens[i];
			if (repLen < 2)
				continue;
			int price = repMatchPrice + GetPureRepPrice(i, _state, posState);
			do
			{
				int curAndLenPrice = price + _repMatchLenEncoder.GetPrice(repLen - 2, posState);
				Optimal optimum = _optimum[repLen];
				if (curAndLenPrice < optimum.Price)
				{
					optimum.Price = curAndLenPrice;
					optimum.PosPrev = 0;
					optimum.BackPrev = i;
					optimum.Prev1IsChar = false;
				}
			}
			while (--repLen >= 2);
		}

		int normalMatchPrice = matchPrice + chapter03.exercises.Encoder2.GetPrice0(_isRep[_state]);

		len = ((repLens[0] >= 2) ? repLens[0] + 1 : 2);
		if (len <= lenMain)
		{
			int offs = 0;
			while (len > _matchDistances[offs])
				offs += 2;
			for (; ; len++)
			{
				int distance = _matchDistances[offs + 1];
				int curAndLenPrice = normalMatchPrice + GetPosLenPrice(distance, len, posState);
				Optimal optimum = _optimum[len];
				if (curAndLenPrice < optimum.Price)
				{
					optimum.Price = curAndLenPrice;
					optimum.PosPrev = 0;
					optimum.BackPrev = distance + Base.kNumRepDistances;
					optimum.Prev1IsChar = false;
				}
				if (len == _matchDistances[offs])
				{
					offs += 2;
					if (offs == numDistancePairs)
						break;
				}
			}
		}

		int cur = 0;

		while (true)
		{
			cur++;
			if (cur == lenEnd)
				return Backward(cur);
			int newLen = ReadMatchDistances();
			numDistancePairs = _numDistancePairs;
			if (newLen >= _numFastBytes)
			{

				_longestMatchLength = newLen;
				_longestMatchWasFound = true;
				return Backward(cur);
			}
			position++;
			int posPrev = _optimum[cur].PosPrev;
			int state;
			if (_optimum[cur].Prev1IsChar)
			{
				posPrev--;
				if (_optimum[cur].Prev2)
				{
					state = _optimum[_optimum[cur].PosPrev2].State;
					if (_optimum[cur].BackPrev2 < Base.kNumRepDistances)
						state = Base.StateUpdateRep(state);
					else
						state = Base.StateUpdateMatch(state);
				}
				else
					state = _optimum[posPrev].State;
				state = Base.StateUpdateChar(state);
			}
			else
				state = _optimum[posPrev].State;
			if (posPrev == cur - 1)
			{
				if (_optimum[cur].IsShortRep())
					state = Base.StateUpdateShortRep(state);
				else
					state = Base.StateUpdateChar(state);
			}
			else
			{
				int pos;
				if (_optimum[cur].Prev1IsChar && _optimum[cur].Prev2)
				{
					posPrev = _optimum[cur].PosPrev2;
					pos = _optimum[cur].BackPrev2;
					state = Base.StateUpdateRep(state);
				}
				else
				{
					pos = _optimum[cur].BackPrev;
					if (pos < Base.kNumRepDistances)
						state = Base.StateUpdateRep(state);
					else
						state = Base.StateUpdateMatch(state);
				}
				Optimal opt = _optimum[posPrev];
				if (pos < Base.kNumRepDistances)
				{
					if (pos == 0)
					{
						reps[0] = opt.Backs0;
						reps[1] = opt.Backs1;
						reps[2] = opt.Backs2;
						reps[3] = opt.Backs3;
					}
					else if (pos == 1)
					{
						reps[0] = opt.Backs1;
						reps[1] = opt.Backs0;
						reps[2] = opt.Backs2;
						reps[3] = opt.Backs3;
					}
					else if (pos == 2)
					{
						reps[0] = opt.Backs2;
						reps[1] = opt.Backs0;
						reps[2] = opt.Backs1;
						reps[3] = opt.Backs3;
					}
					else
					{
						reps[0] = opt.Backs3;
						reps[1] = opt.Backs0;
						reps[2] = opt.Backs1;
						reps[3] = opt.Backs2;
					}
				}
				else
				{
					reps[0] = (pos - Base.kNumRepDistances);
					reps[1] = opt.Backs0;
					reps[2] = opt.Backs1;
					reps[3] = opt.Backs2;
				}
			}
			_optimum[cur].State = state;
			_optimum[cur].Backs0 = reps[0];
			_optimum[cur].Backs1 = reps[1];
			_optimum[cur].Backs2 = reps[2];
			_optimum[cur].Backs3 = reps[3];
			int curPrice = _optimum[cur].Price;

			currentByte = _matchFinder.GetIndexByte(0 - 1);
			matchByte = _matchFinder.GetIndexByte(0 - reps[0] - 1 - 1);

			posState = (position & _posStateMask);

			int curAnd1Price = curPrice +
				chapter03.exercises.Encoder2.GetPrice0(_isMatch[(state << Base.kNumPosStatesBitsMax) + posState]) +
				_literalEncoder.GetSubCoder(position, _matchFinder.GetIndexByte(0 - 2)).
				GetPrice(!Base.StateIsCharState(state), matchByte, currentByte);

			Optimal nextOptimum = _optimum[cur + 1];

			boolean nextIsChar = false;
			if (curAnd1Price < nextOptimum.Price)
			{
				nextOptimum.Price = curAnd1Price;
				nextOptimum.PosPrev = cur;
				nextOptimum.MakeAsChar();
				nextIsChar = true;
			}

			matchPrice = curPrice + chapter03.exercises.Encoder2.GetPrice1(_isMatch[(state << Base.kNumPosStatesBitsMax) + posState]);
			repMatchPrice = matchPrice + chapter03.exercises.Encoder2.GetPrice1(_isRep[state]);

			if (matchByte == currentByte &&
				!(nextOptimum.PosPrev < cur && nextOptimum.BackPrev == 0))
			{
				int shortRepPrice = repMatchPrice + GetRepLen1Price(state, posState);
				if (shortRepPrice <= nextOptimum.Price)
				{
					nextOptimum.Price = shortRepPrice;
					nextOptimum.PosPrev = cur;
					nextOptimum.MakeAsShortRep();
					nextIsChar = true;
				}
			}

			int numAvailableBytesFull = _matchFinder.GetNumAvailableBytes() + 1;
			numAvailableBytesFull = Math.min(kNumOpts - 1 - cur, numAvailableBytesFull);
			numAvailableBytes = numAvailableBytesFull;

			if (numAvailableBytes < 2)
				continue;
			if (numAvailableBytes > _numFastBytes)
				numAvailableBytes = _numFastBytes;
			if (!nextIsChar && matchByte != currentByte)
			{
				// try Literal + rep0
				int t = Math.min(numAvailableBytesFull - 1, _numFastBytes);
				int lenTest2 = _matchFinder.GetMatchLen(0, reps[0], t);
				if (lenTest2 >= 2)
				{
					int state2 = Base.StateUpdateChar(state);

					int posStateNext = (position + 1) & _posStateMask;
					int nextRepMatchPrice = curAnd1Price +
						chapter03.exercises.Encoder2.GetPrice1(_isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext]) +
						chapter03.exercises.Encoder2.GetPrice1(_isRep[state2]);
					{
						int offset = cur + 1 + lenTest2;
						while (lenEnd < offset)
							_optimum[++lenEnd].Price = kIfinityPrice;
						int curAndLenPrice = nextRepMatchPrice + GetRepPrice(
								0, lenTest2, state2, posStateNext);
						Optimal optimum = _optimum[offset];
						if (curAndLenPrice < optimum.Price)
						{
							optimum.Price = curAndLenPrice;
							optimum.PosPrev = cur + 1;
							optimum.BackPrev = 0;
							optimum.Prev1IsChar = true;
							optimum.Prev2 = false;
						}
					}
				}
			}

			int startLen = 2; // speed optimization 

			for (int repIndex = 0; repIndex < Base.kNumRepDistances; repIndex++)
			{
				int lenTest = _matchFinder.GetMatchLen(0 - 1, reps[repIndex], numAvailableBytes);
				if (lenTest < 2)
					continue;
				int lenTestTemp = lenTest;
				do
				{
					while (lenEnd < cur + lenTest)
						_optimum[++lenEnd].Price = kIfinityPrice;
					int curAndLenPrice = repMatchPrice + GetRepPrice(repIndex, lenTest, state, posState);
					Optimal optimum = _optimum[cur + lenTest];
					if (curAndLenPrice < optimum.Price)
					{
						optimum.Price = curAndLenPrice;
						optimum.PosPrev = cur;
						optimum.BackPrev = repIndex;
						optimum.Prev1IsChar = false;
					}
				}
				while (--lenTest >= 2);
				lenTest = lenTestTemp;

				if (repIndex == 0)
					startLen = lenTest + 1;

				// if (_maxMode)
				if (lenTest < numAvailableBytesFull)
				{
					int t = Math.min(numAvailableBytesFull - 1 - lenTest, _numFastBytes);
					int lenTest2 = _matchFinder.GetMatchLen(lenTest, reps[repIndex], t);
					if (lenTest2 >= 2)
					{
						int state2 = Base.StateUpdateRep(state);

						int posStateNext = (position + lenTest) & _posStateMask;
						int curAndLenCharPrice =
								repMatchPrice + GetRepPrice(repIndex, lenTest, state, posState) +
								chapter03.exercises.Encoder2.GetPrice0(_isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext]) +
								_literalEncoder.GetSubCoder(position + lenTest,
								_matchFinder.GetIndexByte(lenTest - 1 - 1)).GetPrice(true,
								_matchFinder.GetIndexByte(lenTest - 1 - (reps[repIndex] + 1)),
								_matchFinder.GetIndexByte(lenTest - 1));
						state2 = Base.StateUpdateChar(state2);
						posStateNext = (position + lenTest + 1) & _posStateMask;
						int nextMatchPrice = curAndLenCharPrice + chapter03.exercises.Encoder2.GetPrice1(_isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext]);
						int nextRepMatchPrice = nextMatchPrice + chapter03.exercises.Encoder2.GetPrice1(_isRep[state2]);

						// for(; lenTest2 >= 2; lenTest2--)
						{
							int offset = lenTest + 1 + lenTest2;
							while (lenEnd < cur + offset)
								_optimum[++lenEnd].Price = kIfinityPrice;
							int curAndLenPrice = nextRepMatchPrice + GetRepPrice(0, lenTest2, state2, posStateNext);
							Optimal optimum = _optimum[cur + offset];
							if (curAndLenPrice < optimum.Price)
							{
								optimum.Price = curAndLenPrice;
								optimum.PosPrev = cur + lenTest + 1;
								optimum.BackPrev = 0;
								optimum.Prev1IsChar = true;
								optimum.Prev2 = true;
								optimum.PosPrev2 = cur;
								optimum.BackPrev2 = repIndex;
							}
						}
					}
				}
			}

			if (newLen > numAvailableBytes)
			{
				newLen = numAvailableBytes;
				for (numDistancePairs = 0; newLen > _matchDistances[numDistancePairs]; numDistancePairs += 2) ;
				_matchDistances[numDistancePairs] = newLen;
				numDistancePairs += 2;
			}
			if (newLen >= startLen)
			{
				normalMatchPrice = matchPrice + chapter03.exercises.Encoder2.GetPrice0(_isRep[state]);
				while (lenEnd < cur + newLen)
					_optimum[++lenEnd].Price = kIfinityPrice;

				int offs = 0;
				while (startLen > _matchDistances[offs])
					offs += 2;

				for (int lenTest = startLen; ; lenTest++)
				{
					int curBack = _matchDistances[offs + 1];
					int curAndLenPrice = normalMatchPrice + GetPosLenPrice(curBack, lenTest, posState);
					Optimal optimum = _optimum[cur + lenTest];
					if (curAndLenPrice < optimum.Price)
					{
						optimum.Price = curAndLenPrice;
						optimum.PosPrev = cur;
						optimum.BackPrev = curBack + Base.kNumRepDistances;
						optimum.Prev1IsChar = false;
					}

					if (lenTest == _matchDistances[offs])
					{
						if (lenTest < numAvailableBytesFull)
						{
							int t = Math.min(numAvailableBytesFull - 1 - lenTest, _numFastBytes);
							int lenTest2 = _matchFinder.GetMatchLen(lenTest, curBack, t);
							if (lenTest2 >= 2)
							{
								int state2 = Base.StateUpdateMatch(state);

								int posStateNext = (position + lenTest) & _posStateMask;
								int curAndLenCharPrice = curAndLenPrice +
									chapter03.exercises.Encoder2.GetPrice0(_isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext]) +
									_literalEncoder.GetSubCoder(position + lenTest,
									_matchFinder.GetIndexByte(lenTest - 1 - 1)).
									GetPrice(true,
									_matchFinder.GetIndexByte(lenTest - (curBack + 1) - 1),
									_matchFinder.GetIndexByte(lenTest - 1));
								state2 = Base.StateUpdateChar(state2);
								posStateNext = (position + lenTest + 1) & _posStateMask;
								int nextMatchPrice = curAndLenCharPrice + chapter03.exercises.Encoder2.GetPrice1(_isMatch[(state2 << Base.kNumPosStatesBitsMax) + posStateNext]);
								int nextRepMatchPrice = nextMatchPrice + chapter03.exercises.Encoder2.GetPrice1(_isRep[state2]);

								int offset = lenTest + 1 + lenTest2;
								while (lenEnd < cur + offset)
									_optimum[++lenEnd].Price = kIfinityPrice;
								curAndLenPrice = nextRepMatchPrice + GetRepPrice(0, lenTest2, state2, posStateNext);
								optimum = _optimum[cur + offset];
								if (curAndLenPrice < optimum.Price)
								{
									optimum.Price = curAndLenPrice;
									optimum.PosPrev = cur + lenTest + 1;
									optimum.BackPrev = 0;
									optimum.Prev1IsChar = true;
									optimum.Prev2 = true;
									optimum.PosPrev2 = cur;
									optimum.BackPrev2 = curBack + Base.kNumRepDistances;
								}
							}
						}
						offs += 2;
						if (offs == numDistancePairs)
							break;
					}
				}
			}
		}
	}

	boolean ChangePair(int smallDist, int bigDist)
	{
		int kDif = 7;
		return (smallDist < (1 << (32 - kDif)) && bigDist >= (smallDist << kDif));
	}

	void WriteEndMarker(int posState) throws IOException
	{
		if (!_writeEndMark)
			return;

		_rangeEncoder.Encode(_isMatch, (_state << Base.kNumPosStatesBitsMax) + posState, 1);
		_rangeEncoder.Encode(_isRep, _state, 0);
		_state = Base.StateUpdateMatch(_state);
		int len = Base.kMatchMinLen;
		_lenEncoder.Encode(_rangeEncoder, len - Base.kMatchMinLen, posState);
		int posSlot = (1 << Base.kNumPosSlotBits) - 1;
		int lenToPosState = Base.GetLenToPosState(len);
		_posSlotEncoder[lenToPosState].Encode(_rangeEncoder, posSlot);
		int footerBits = 30;
		int posReduced = (1 << footerBits) - 1;
		_rangeEncoder.EncodeDirectBits(posReduced >> Base.kNumAlignBits, footerBits - Base.kNumAlignBits);
		_posAlignEncoder.ReverseEncode(_rangeEncoder, posReduced & Base.kAlignMask);
	}

	void Flush(int nowPos) throws IOException
	{
		ReleaseMFStream();
		WriteEndMarker(nowPos & _posStateMask);
		_rangeEncoder.FlushData();
		_rangeEncoder.FlushStream();
	}

	public void CodeOneBlock(long[] inSize, long[] outSize, boolean[] finished) throws IOException
	{
		inSize[0] = 0;
		outSize[0] = 0;
		finished[0] = true;

		if (_inStream != null)
		{
			_matchFinder.SetStream(_inStream);
			_matchFinder.Init();
			_needReleaseMFStream = true;
			_inStream = null;
		}

		if (_finished)
			return;
		_finished = true;


		long progressPosValuePrev = nowPos64;
		if (nowPos64 == 0)
		{
			if (_matchFinder.GetNumAvailableBytes() == 0)
			{
				Flush((int)nowPos64);
				return;
			}

			ReadMatchDistances();
			int posState = (int)(nowPos64) & _posStateMask;
			_rangeEncoder.Encode(_isMatch, (_state << Base.kNumPosStatesBitsMax) + posState, 0);
			_state = Base.StateUpdateChar(_state);
			byte curByte = _matchFinder.GetIndexByte(0 - _additionalOffset);
			_literalEncoder.GetSubCoder((int)(nowPos64), _previousByte).Encode(_rangeEncoder, curByte);
			_previousByte = curByte;
			_additionalOffset--;
			nowPos64++;
		}
		if (_matchFinder.GetNumAvailableBytes() == 0)
		{
			Flush((int)nowPos64);
			return;
		}
		while (true)
		{

			int len = GetOptimum((int)nowPos64);
			int pos = backRes;
			int posState = ((int)nowPos64) & _posStateMask;
			int complexState = (_state << Base.kNumPosStatesBitsMax) + posState;
			if (len == 1 && pos == -1)
			{
				_rangeEncoder.Encode(_isMatch, complexState, 0);
				byte curByte = _matchFinder.GetIndexByte((int)(0 - _additionalOffset));
				LiteralEncoder.Encoder2 subCoder = _literalEncoder.GetSubCoder((int)nowPos64, _previousByte);
				if (!Base.StateIsCharState(_state))
				{
					byte matchByte = _matchFinder.GetIndexByte((int)(0 - _repDistances[0] - 1 - _additionalOffset));
					subCoder.EncodeMatched(_rangeEncoder, matchByte, curByte);
				}
				else
					subCoder.Encode(_rangeEncoder, curByte);
				_previousByte = curByte;
				_state = Base.StateUpdateChar(_state);
			}
			else
			{
				_rangeEncoder.Encode(_isMatch, complexState, 1);
				if (pos < Base.kNumRepDistances)
				{
					_rangeEncoder.Encode(_isRep, _state, 1);
					if (pos == 0)
					{
						_rangeEncoder.Encode(_isRepG0, _state, 0);
						if (len == 1)
							_rangeEncoder.Encode(_isRep0Long, complexState, 0);
						else
							_rangeEncoder.Encode(_isRep0Long, complexState, 1);
					}
					else
					{
						_rangeEncoder.Encode(_isRepG0, _state, 1);
						if (pos == 1)
							_rangeEncoder.Encode(_isRepG1, _state, 0);
						else
						{
							_rangeEncoder.Encode(_isRepG1, _state, 1);
							_rangeEncoder.Encode(_isRepG2, _state, pos - 2);
						}
					}
					if (len == 1)
						_state = Base.StateUpdateShortRep(_state);
					else
					{
						_repMatchLenEncoder.Encode(_rangeEncoder, len - Base.kMatchMinLen, posState);
						_state = Base.StateUpdateRep(_state);
					}
					int distance = _repDistances[pos];
					if (pos != 0)
					{
						for (int i = pos; i >= 1; i--)
							_repDistances[i] = _repDistances[i - 1];
						_repDistances[0] = distance;
					}
				}
				else
				{
					_rangeEncoder.Encode(_isRep, _state, 0);
					_state = Base.StateUpdateMatch(_state);
					_lenEncoder.Encode(_rangeEncoder, len - Base.kMatchMinLen, posState);
					pos -= Base.kNumRepDistances;
					int posSlot = GetPosSlot(pos);
					int lenToPosState = Base.GetLenToPosState(len);
					_posSlotEncoder[lenToPosState].Encode(_rangeEncoder, posSlot);

					if (posSlot >= Base.kStartPosModelIndex)
					{
						int footerBits = (int)((posSlot >> 1) - 1);
						int baseVal = ((2 | (posSlot & 1)) << footerBits);
						int posReduced = pos - baseVal;

						if (posSlot < Base.kEndPosModelIndex)
							BitTreeEncoder.ReverseEncode(_posEncoders,
									baseVal - posSlot - 1, _rangeEncoder, footerBits, posReduced);
						else
						{
							_rangeEncoder.EncodeDirectBits(posReduced >> Base.kNumAlignBits, footerBits - Base.kNumAlignBits);
							_posAlignEncoder.ReverseEncode(_rangeEncoder, posReduced & Base.kAlignMask);
							_alignPriceCount++;
						}
					}
					int distance = pos;
					for (int i = Base.kNumRepDistances - 1; i >= 1; i--)
						_repDistances[i] = _repDistances[i - 1];
					_repDistances[0] = distance;
					_matchPriceCount++;
				}
				_previousByte = _matchFinder.GetIndexByte(len - 1 - _additionalOffset);
			}
			_additionalOffset -= len;
			nowPos64 += len;
			if (_additionalOffset == 0)
			{
				// if (!_fastMode)
				if (_matchPriceCount >= (1 << 7))
					FillDistancesPrices();
				if (_alignPriceCount >= Base.kAlignTableSize)
					FillAlignPrices();
				inSize[0] = nowPos64;
				outSize[0] = _rangeEncoder.GetProcessedSizeAdd();
				if (_matchFinder.GetNumAvailableBytes() == 0)
				{
					Flush((int)nowPos64);
					return;
				}

				if (nowPos64 - progressPosValuePrev >= (1 << 12))
				{
					_finished = false;
					finished[0] = false;
					return;
				}
			}
		}
	}

	void ReleaseMFStream()
	{
		if (_matchFinder != null && _needReleaseMFStream)
		{
			_matchFinder.ReleaseStream();
			_needReleaseMFStream = false;
		}
	}

	void SetOutStream(java.io.OutputStream outStream)
	{ _rangeEncoder.SetStream(outStream); }
	void ReleaseOutStream()
	{ _rangeEncoder.ReleaseStream(); }

	void ReleaseStreams()
	{
		ReleaseMFStream();
		ReleaseOutStream();
	}

	void SetStreams(java.io.InputStream inStream, java.io.OutputStream outStream,
			long inSize, long outSize)
	{
		_inStream = inStream;
		_finished = false;
		Create();
		SetOutStream(outStream);
		Init();

		// if (!_fastMode)
		{
			FillDistancesPrices();
			FillAlignPrices();
		}

		_lenEncoder.SetTableSize(_numFastBytes + 1 - Base.kMatchMinLen);
		_lenEncoder.UpdateTables(1 << _posStateBits);
		_repMatchLenEncoder.SetTableSize(_numFastBytes + 1 - Base.kMatchMinLen);
		_repMatchLenEncoder.UpdateTables(1 << _posStateBits);

		nowPos64 = 0;
	}

	long[] processedInSize = new long[1]; long[] processedOutSize = new long[1]; boolean[] finished = new boolean[1];
	public void Code(java.io.InputStream inStream, java.io.OutputStream outStream,
			long inSize, long outSize, ICodeProgress progress) throws IOException
	{
		_needReleaseMFStream = false;
		try
		{
			SetStreams(inStream, outStream, inSize, outSize);
			while (true)
			{



				CodeOneBlock(processedInSize, processedOutSize, finished);
				if (finished[0])
					return;
				if (progress != null)
				{
					progress.SetProgress(processedInSize[0], processedOutSize[0]);
				}
			}
		}
		finally
		{
			ReleaseStreams();
		}
	}

	public static final int kPropSize = 5;
	byte[] properties = new byte[kPropSize];

	public void WriteCoderProperties(java.io.OutputStream outStream) throws IOException
	{
		properties[0] = (byte)((_posStateBits * 5 + _numLiteralPosStateBits) * 9 + _numLiteralContextBits);
		for (int i = 0; i < 4; i++)
			properties[1 + i] = (byte)(_dictionarySize >> (8 * i));
		outStream.write(properties, 0, kPropSize);
	}

	int[] tempPrices = new int[Base.kNumFullDistances];
	int _matchPriceCount;

	void FillDistancesPrices()
	{
		for (int i = Base.kStartPosModelIndex; i < Base.kNumFullDistances; i++)
		{
			int posSlot = GetPosSlot(i);
			int footerBits = (int)((posSlot >> 1) - 1);
			int baseVal = ((2 | (posSlot & 1)) << footerBits);
			tempPrices[i] = BitTreeEncoder.ReverseGetPrice(_posEncoders,
				baseVal - posSlot - 1, footerBits, i - baseVal);
		}

		for (int lenToPosState = 0; lenToPosState < Base.kNumLenToPosStates; lenToPosState++)
		{
			int posSlot;
			BitTreeEncoder encoder = _posSlotEncoder[lenToPosState];

			int st = (lenToPosState << Base.kNumPosSlotBits);
			for (posSlot = 0; posSlot < _distTableSize; posSlot++)
				_posSlotPrices[st + posSlot] = encoder.GetPrice(posSlot);
			for (posSlot = Base.kEndPosModelIndex; posSlot < _distTableSize; posSlot++)
				_posSlotPrices[st + posSlot] += ((((posSlot >> 1) - 1) - Base.kNumAlignBits) << chapter03.exercises.Encoder2.kNumBitPriceShiftBits);

			int st2 = lenToPosState * Base.kNumFullDistances;
			int i;
			for (i = 0; i < Base.kStartPosModelIndex; i++)
				_distancesPrices[st2 + i] = _posSlotPrices[st + i];
			for (; i < Base.kNumFullDistances; i++)
				_distancesPrices[st2 + i] = _posSlotPrices[st + GetPosSlot(i)] + tempPrices[i];
		}
		_matchPriceCount = 0;
	}

	void FillAlignPrices()
	{
		for (int i = 0; i < Base.kAlignTableSize; i++)
			_alignPrices[i] = _posAlignEncoder.ReverseGetPrice(i);
		_alignPriceCount = 0;
	}


	public boolean SetAlgorithm(int algorithm)
	{
		/*
		_fastMode = (algorithm == 0);
		_maxMode = (algorithm >= 2);
		*/
		return true;
	}

	public boolean SetDictionarySize(int dictionarySize)
	{
		int kDicLogSizeMaxCompress = 29;
		if (dictionarySize < (1 << Base.kDicLogSizeMin) || dictionarySize > (1 << kDicLogSizeMaxCompress))
			return false;
		_dictionarySize = dictionarySize;
		int dicLogSize;
		for (dicLogSize = 0; dictionarySize > (1 << dicLogSize); dicLogSize++) ;
		_distTableSize = dicLogSize * 2;
		return true;
	}

	public boolean SetNumFastBytes(int numFastBytes)
	{
		if (numFastBytes < 5 || numFastBytes > Base.kMatchMaxLen)
			return false;
		_numFastBytes = numFastBytes;
		return true;
	}

	public boolean SetMatchFinder(int matchFinderIndex)
	{
		if (matchFinderIndex < 0 || matchFinderIndex > 2)
			return false;
		int matchFinderIndexPrev = _matchFinderType;
		_matchFinderType = matchFinderIndex;
		if (_matchFinder != null && matchFinderIndexPrev != _matchFinderType)
		{
			_dictionarySizePrev = -1;
			_matchFinder = null;
		}
		return true;
	}

	public boolean SetLcLpPb(int lc, int lp, int pb)
	{
		if (
				lp < 0 || lp > Base.kNumLitPosStatesBitsEncodingMax ||
				lc < 0 || lc > Base.kNumLitContextBitsMax ||
				pb < 0 || pb > Base.kNumPosStatesBitsEncodingMax)
			return false;
		_numLiteralPosStateBits = lp;
		_numLiteralContextBits = lc;
		_posStateBits = pb;
		_posStateMask = ((1) << _posStateBits) - 1;
		return true;
	}

	public void SetEndMarkerMode(boolean endMarkerMode)
	{
		_writeEndMark = endMarkerMode;
	}
}

class LzmaBench
{
	static final int kAdditionalSize = (1 << 21);
	static final int kCompressedAdditionalSize = (1 << 10);
	
	static class CRandomGenerator
	{
		int A1;
		int A2;
		public CRandomGenerator() { Init(); }
		public void Init() { A1 = 362436069; A2 = 521288629; }
		public int GetRnd()
		{
			return
				((A1 = 36969 * (A1 & 0xffff) + (A1 >>> 16)) << 16) ^
				((A2 = 18000 * (A2 & 0xffff) + (A2 >>> 16)));
		}
	};
	
	static class CBitRandomGenerator
	{
		CRandomGenerator RG = new CRandomGenerator();
		int Value;
		int NumBits;
		public void Init()
		{
			Value = 0;
			NumBits = 0;
		}
		public int GetRnd(int numBits)
		{
			int result;
			if (NumBits > numBits)
			{
				result = Value & ((1 << numBits) - 1);
				Value >>>= numBits;
				NumBits -= numBits;
				return result;
			}
			numBits -= NumBits;
			result = (Value << numBits);
			Value = RG.GetRnd();
			result |= Value & (((int)1 << numBits) - 1);
			Value >>>= numBits;
			NumBits = 32 - numBits;
			return result;
		}
	};
	
	static class CBenchRandomGenerator
	{
		CBitRandomGenerator RG = new CBitRandomGenerator();
		int Pos;
		int Rep0;

		public int BufferSize;
		public byte[] Buffer = null;

		public CBenchRandomGenerator() { }
		public void Set(int bufferSize)
		{
			Buffer = new byte[bufferSize];
			Pos = 0;
			BufferSize = bufferSize;
		}
		int GetRndBit() { return RG.GetRnd(1); }
		int GetLogRandBits(int numBits)
		{
			int len = RG.GetRnd(numBits);
			return RG.GetRnd((int)len);
		}
		int GetOffset()
		{
			if (GetRndBit() == 0)
				return GetLogRandBits(4);
			return (GetLogRandBits(4) << 10) | RG.GetRnd(10);
		}
		int GetLen1() { return RG.GetRnd(1 + (int)RG.GetRnd(2)); }
		int GetLen2() { return RG.GetRnd(2 + (int)RG.GetRnd(2)); }
		public void Generate()
		{
			RG.Init();
			Rep0 = 1;
			while (Pos < BufferSize)
			{
				if (GetRndBit() == 0 || Pos < 1)
					Buffer[Pos++] = (byte)(RG.GetRnd(8));
				else
				{
					int len;
					if (RG.GetRnd(3) == 0)
						len = 1 + GetLen1();
					else
					{
						do
							Rep0 = GetOffset();
						while (Rep0 >= Pos);
						Rep0++;
						len = 2 + GetLen2();
					}
					for (int i = 0; i < len && Pos < BufferSize; i++, Pos++)
						Buffer[Pos] = Buffer[Pos - Rep0];
				}
			}
		}
	};
	
	static class CrcOutStream extends java.io.OutputStream
	{
		public CRC CRC = new CRC();
		
		public void Init()
		{ 
			CRC.Init(); 
		}
		public int GetDigest()
		{ 
			return CRC.GetDigest(); 
		}
		public void write(byte[] b)
		{
			CRC.Update(b);
		}
		public void write(byte[] b, int off, int len)
		{
			CRC.Update(b, off, len);
		}
		public void write(int b)
		{
			CRC.UpdateByte(b);
		}
	};

	static class MyOutputStream extends java.io.OutputStream
	{
		byte[] _buffer;
		int _size;
		int _pos;
		
		public MyOutputStream(byte[] buffer)
		{
			_buffer = buffer;
			_size = _buffer.length;
		}
		
		public void reset()
		{ 
			_pos = 0; 
		}
		
		public void write(int b) throws IOException
		{
			if (_pos >= _size)
				throw new IOException("Error");
			_buffer[_pos++] = (byte)b;
		}
		
		public int size()
		{
			return _pos;
		}
	};

	static class MyInputStream extends java.io.InputStream
	{
		byte[] _buffer;
		int _size;
		int _pos;
		
		public MyInputStream(byte[] buffer, int size)
		{
			_buffer = buffer;
			_size = size;
		}
		
		public void reset()
		{ 
			_pos = 0; 
		}
		
		public int read()
		{
			if (_pos >= _size)
				return -1;
			return _buffer[_pos++] & 0xFF;
		}
	};
	
	static class CProgressInfo implements ICodeProgress
	{
		public long ApprovedStart;
		public long InSize;
		public long Time;
		public void Init()
		{ InSize = 0; }
		public void SetProgress(long inSize, long outSize)
		{
			if (inSize >= ApprovedStart && InSize == 0)
			{
				Time = System.currentTimeMillis();
				InSize = inSize;
			}
		}
	}
	static final int kSubBits = 8;
	
	static int GetLogSize(int size)
	{
		for (int i = kSubBits; i < 32; i++)
			for (int j = 0; j < (1 << kSubBits); j++)
				if (size <= ((1) << i) + (j << (i - kSubBits)))
					return (i << kSubBits) + j;
		return (32 << kSubBits);
	}
	
	static long MyMultDiv64(long value, long elapsedTime)
	{
		long freq = 1000; // ms
		long elTime = elapsedTime;
		while (freq > 1000000)
		{
			freq >>>= 1;
			elTime >>>= 1;
		}
		if (elTime == 0)
			elTime = 1;
		return value * freq / elTime;
	}
	
	static long GetCompressRating(int dictionarySize, long elapsedTime, long size)
	{
		long t = GetLogSize(dictionarySize) - (18 << kSubBits);
		long numCommandsForOne = 1060 + ((t * t * 10) >> (2 * kSubBits));
		long numCommands = (long)(size) * numCommandsForOne;
		return MyMultDiv64(numCommands, elapsedTime);
	}
	
	static long GetDecompressRating(long elapsedTime, long outSize, long inSize)
	{
		long numCommands = inSize * 220 + outSize * 20;
		return MyMultDiv64(numCommands, elapsedTime);
	}
	
	static long GetTotalRating(
			int dictionarySize,
			long elapsedTimeEn, long sizeEn,
			long elapsedTimeDe,
			long inSizeDe, long outSizeDe)
	{
		return (GetCompressRating(dictionarySize, elapsedTimeEn, sizeEn) +
				GetDecompressRating(elapsedTimeDe, inSizeDe, outSizeDe)) / 2;
	}
	
	static void PrintValue(long v)
	{
		String s = "";
		s += v;
		for (int i = 0; i + s.length() < 6; i++)
			System.out.print(" ");
		System.out.print(s);
	}
	
	static void PrintRating(long rating)
	{
		PrintValue(rating / 1000000);
		System.out.print(" MIPS");
	}
	
	static void PrintResults(
			int dictionarySize,
			long elapsedTime,
			long size,
			boolean decompressMode, long secondSize)
	{
		long speed = MyMultDiv64(size, elapsedTime);
		PrintValue(speed / 1024);
		System.out.print(" KB/s  ");
		long rating;
		if (decompressMode)
			rating = GetDecompressRating(elapsedTime, size, secondSize);
		else
			rating = GetCompressRating(dictionarySize, elapsedTime, size);
		PrintRating(rating);
	}
	
	static public int LzmaBenchmark(int numIterations, int dictionarySize) throws Exception
	{
		if (numIterations <= 0)
			return 0;
		if (dictionarySize < (1 << 18))
		{
			System.out.println("\nError: dictionary size for benchmark must be >= 18 (256 KB)");
			return 1;
		}
		System.out.print("\n       Compressing                Decompressing\n\n");
		
		chapter03.exercises.Encoder encoder = new chapter03.exercises.Encoder();
		chapter03.exercises.Decoder decoder = new chapter03.exercises.Decoder();
		
		if (!encoder.SetDictionarySize(dictionarySize))
			throw new Exception("Incorrect dictionary size");
		
		int kBufferSize = dictionarySize + kAdditionalSize;
		int kCompressedBufferSize = (kBufferSize / 2) + kCompressedAdditionalSize;
		
		ByteArrayOutputStream propStream = new ByteArrayOutputStream();
		encoder.WriteCoderProperties(propStream);
		byte[] propArray = propStream.toByteArray();
		decoder.SetDecoderProperties(propArray);
		
		CBenchRandomGenerator rg = new CBenchRandomGenerator();

		rg.Set(kBufferSize);
		rg.Generate();
		CRC crc = new CRC();
		crc.Init();
		crc.Update(rg.Buffer, 0, rg.BufferSize);
		
		CProgressInfo progressInfo = new CProgressInfo();
		progressInfo.ApprovedStart = dictionarySize;
		
		long totalBenchSize = 0;
		long totalEncodeTime = 0;
		long totalDecodeTime = 0;
		long totalCompressedSize = 0;
		
		MyInputStream inStream = new MyInputStream(rg.Buffer, rg.BufferSize);

		byte[] compressedBuffer = new byte[kCompressedBufferSize];
		MyOutputStream compressedStream = new MyOutputStream(compressedBuffer);
		CrcOutStream crcOutStream = new CrcOutStream();
		MyInputStream inputCompressedStream = null;
		int compressedSize = 0;
		for (int i = 0; i < numIterations; i++)
		{
			progressInfo.Init();
			inStream.reset();
			compressedStream.reset();
			encoder.Code(inStream, compressedStream, -1, -1, progressInfo);
			long encodeTime = System.currentTimeMillis() - progressInfo.Time;
			
			if (i == 0)
			{
				compressedSize = compressedStream.size();
				inputCompressedStream = new MyInputStream(compressedBuffer, compressedSize);
			}
			else if (compressedSize != compressedStream.size())
				throw (new Exception("Encoding error"));
				
			if (progressInfo.InSize == 0)
				throw (new Exception("Internal ERROR 1282"));

			long decodeTime = 0;
			for (int j = 0; j < 2; j++)
			{
				inputCompressedStream.reset();
				crcOutStream.Init();
				
				long outSize = kBufferSize;
				long startTime = System.currentTimeMillis();
				if (!decoder.Code(inputCompressedStream, crcOutStream, outSize))
					throw (new Exception("Decoding Error"));;
				decodeTime = System.currentTimeMillis() - startTime;
				if (crcOutStream.GetDigest() != crc.GetDigest())
					throw (new Exception("CRC Error"));
			}
			long benchSize = kBufferSize - (long)progressInfo.InSize;
			PrintResults(dictionarySize, encodeTime, benchSize, false, 0);
			System.out.print("     ");
			PrintResults(dictionarySize, decodeTime, kBufferSize, true, compressedSize);
			System.out.println();
			
			totalBenchSize += benchSize;
			totalEncodeTime += encodeTime;
			totalDecodeTime += decodeTime;
			totalCompressedSize += compressedSize;
		}
		System.out.println("---------------------------------------------------");
		PrintResults(dictionarySize, totalEncodeTime, totalBenchSize, false, 0);
		System.out.print("     ");
		PrintResults(dictionarySize, totalDecodeTime,
				kBufferSize * (long)numIterations, true, totalCompressedSize);
		System.out.println("    Average");
		return 0;
	}
}

public class LzmaAlone
{
	static public class CommandLine
	{
		public static final int kEncode = 0;
		public static final int kDecode = 1;
		public static final int kBenchmak = 2;
		
		public int Command = -1;
		public int NumBenchmarkPasses = 10;
		
		public int DictionarySize = 1 << 23;
		public boolean DictionarySizeIsDefined = false;
		
		public int Lc = 3;
		public int Lp = 0;
		public int Pb = 2;
		
		public int Fb = 128;
		public boolean FbIsDefined = false;
		
		public boolean Eos = false;
		
		public int Algorithm = 2;
		public int MatchFinder = 1;
		
		public String InFile;
		public String OutFile;
		
		boolean ParseSwitch(String s)
		{
			if (s.startsWith("d"))
			{
				DictionarySize = 1 << Integer.parseInt(s.substring(1));
				DictionarySizeIsDefined = true;
			}
			else if (s.startsWith("fb"))
			{
				Fb = Integer.parseInt(s.substring(2));
				FbIsDefined = true;
			}
			else if (s.startsWith("a"))
				Algorithm = Integer.parseInt(s.substring(1));
			else if (s.startsWith("lc"))
				Lc = Integer.parseInt(s.substring(2));
			else if (s.startsWith("lp"))
				Lp = Integer.parseInt(s.substring(2));
			else if (s.startsWith("pb"))
				Pb = Integer.parseInt(s.substring(2));
			else if (s.startsWith("eos"))
				Eos = true;
			else if (s.startsWith("mf"))
			{
				String mfs = s.substring(2);
				if (mfs.equals("bt2"))
					MatchFinder = 0;
				else if (mfs.equals("bt4"))
					MatchFinder = 1;
				else if (mfs.equals("bt4b"))
					MatchFinder = 2;
				else
					return false;
			}
			else
				return false;
			return true;
		}
		
		public boolean Parse(String[] args) throws Exception
		{
			int pos = 0;
			boolean switchMode = true;
			for (int i = 0; i < args.length; i++)
			{
				String s = args[i];
				if (s.length() == 0)
					return false;
				if (switchMode)
				{
					if (s.compareTo("--") == 0)
					{
						switchMode = false;
						continue;
					}
					if (s.charAt(0) == '-')
					{
						String sw = s.substring(1).toLowerCase();
						if (sw.length() == 0)
							return false;
						try
						{
							if (!ParseSwitch(sw))
								return false;
						}
						catch (NumberFormatException e)
						{
							return false;
						}
						continue;
					}
				}
				if (pos == 0)
				{
					if (s.equalsIgnoreCase("e"))
						Command = kEncode;
					else if (s.equalsIgnoreCase("d"))
						Command = kDecode;
					else if (s.equalsIgnoreCase("b"))
						Command = kBenchmak;
					else
						return false;
				}
				else if(pos == 1)
				{
					if (Command == kBenchmak)
					{
						try
						{
							NumBenchmarkPasses = Integer.parseInt(s);
							if (NumBenchmarkPasses < 1)
								return false;
						}
						catch (NumberFormatException e)
						{
							return false;
						}
					}
					else
						InFile = s;
				}
				else if(pos == 2)
					OutFile = s;
				else
					return false;
				pos++;
				continue;
			}
			return true;
		}
	}
	
	
	static void PrintHelp()
	{
		System.out.println(
				"\nUsage:  LZMA <e|d> [<switches>...] inputFile outputFile\n" +
				"  e: encode file\n" +
				"  d: decode file\n" +
				"  b: Benchmark\n" +
				"<Switches>\n" +
				// "  -a{N}:  set compression mode - [0, 1], default: 1 (max)\n" +
				"  -d{N}:  set dictionary - [0,28], default: 23 (8MB)\n" +
				"  -fb{N}: set number of fast bytes - [5, 273], default: 128\n" +
				"  -lc{N}: set number of literal context bits - [0, 8], default: 3\n" +
				"  -lp{N}: set number of literal pos bits - [0, 4], default: 0\n" +
				"  -pb{N}: set number of pos bits - [0, 4], default: 2\n" +
				"  -mf{MF_ID}: set Match Finder: [bt2, bt4], default: bt4\n" +
				"  -eos:   write End Of Stream marker\n"
				);
	}
	
	public static void main(String[] args) throws Exception
	{
		System.out.println("\nLZMA (Java) 4.61  2008-11-23\n");
		
		if (args.length < 1)
		{
			PrintHelp();
			return;
		}
		
		CommandLine params = new CommandLine();
		if (!params.Parse(args))
		{
			System.out.println("\nIncorrect command");
			return;
		}
		
		if (params.Command == CommandLine.kBenchmak)
		{
			int dictionary = (1 << 21);
			if (params.DictionarySizeIsDefined)
				dictionary = params.DictionarySize;
			if (params.MatchFinder > 1)
				throw new Exception("Unsupported match finder");
			chapter03.exercises.LzmaBench.LzmaBenchmark(params.NumBenchmarkPasses, dictionary);
		}
		else if (params.Command == CommandLine.kEncode || params.Command == CommandLine.kDecode)
		{
			java.io.File inFile = new java.io.File(params.InFile);
			java.io.File outFile = new java.io.File(params.OutFile);
			
			java.io.BufferedInputStream inStream  = new java.io.BufferedInputStream(new java.io.FileInputStream(inFile));
			java.io.BufferedOutputStream outStream = new java.io.BufferedOutputStream(new java.io.FileOutputStream(outFile));
			
			boolean eos = false;
			if (params.Eos)
				eos = true;
			if (params.Command == CommandLine.kEncode)
			{
				chapter03.exercises.Encoder encoder = new chapter03.exercises.Encoder();
				if (!encoder.SetAlgorithm(params.Algorithm))
					throw new Exception("Incorrect compression mode");
				if (!encoder.SetDictionarySize(params.DictionarySize))
					throw new Exception("Incorrect dictionary size");
				if (!encoder.SetNumFastBytes(params.Fb))
					throw new Exception("Incorrect -fb value");
				if (!encoder.SetMatchFinder(params.MatchFinder))
					throw new Exception("Incorrect -mf value");
				if (!encoder.SetLcLpPb(params.Lc, params.Lp, params.Pb))
					throw new Exception("Incorrect -lc or -lp or -pb value");
				encoder.SetEndMarkerMode(eos);
				encoder.WriteCoderProperties(outStream);
				long fileSize;
				if (eos)
					fileSize = -1;
				else
					fileSize = inFile.length();
				for (int i = 0; i < 8; i++)
					outStream.write((int)(fileSize >>> (8 * i)) & 0xFF);
				encoder.Code(inStream, outStream, -1, -1, null);
			}
			else
			{
				int propertiesSize = 5;
				byte[] properties = new byte[propertiesSize];
				if (inStream.read(properties, 0, propertiesSize) != propertiesSize)
					throw new Exception("input .lzma file is too short");
				chapter03.exercises.Decoder decoder = new chapter03.exercises.Decoder();
				if (!decoder.SetDecoderProperties(properties))
					throw new Exception("Incorrect stream properties");
				long outSize = 0;
				for (int i = 0; i < 8; i++)
				{
					int v = inStream.read();
					if (v < 0)
						throw new Exception("Can't read stream size");
					outSize |= ((long)v) << (8 * i);
				}
				if (!decoder.Code(inStream, outStream, outSize))
					throw new Exception("Error in data stream");
			}
			outStream.flush();
			outStream.close();
			inStream.close();
		}
		else
			throw new Exception("Incorrect command");
		return;
	}
}
