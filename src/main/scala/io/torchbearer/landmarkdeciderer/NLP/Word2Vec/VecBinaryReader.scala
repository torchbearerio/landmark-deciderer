// Copyright 2013 trananh
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.torchbearer.landmarkdeciderer.NLP.Word2Vec

import java.io._

import scala.collection.mutable.ArrayBuffer

/** A simple binary file reader.
  * @constructor Create a binary file reader.
  * @param stream An InputStream corresponding to the vectors binary file.
  *
  * @author trananh
  */
class VecBinaryReader(val stream: InputStream) {

  /** ASCII values for common delimiter characters */
  private val SPACE = 32
  private val LF = 10

  /** Open input streams */
  private val dis = new DataInputStream(stream)

  /** Close the stream. */
  def close() { dis.close(); stream.close(); }

  /** Read the next byte.
    * @return The next byte from the file.
    */
  def read(): Byte = dis.readByte()

  /** Read the next token as a string, using the provided delimiters as breaking points.
    * @param delimiters ASCII code of delimiter characters (default to SPACE and LINE-FEED).
    * @return String representation of the next token.
    */
  def readToken(delimiters: Set[Int] = Set(SPACE, LF)): String = {
    val bytes = new ArrayBuffer[Byte]()
    val sb = new StringBuilder()
    var byte = dis.readByte()
    while (!delimiters.contains(byte)) {
      bytes.append(byte)
      byte = dis.readByte()
    }
    sb.append(new String(bytes.toArray[Byte])).toString()
  }

  /** Read next 4 bytes as a floating-point number.
    * @return The floating-point value of the next 4 bytes.
    */
  def readFloat(): Float = {
    // We need to reverse the byte order here due to endian-compatibility.
    java.lang.Float.intBitsToFloat(java.lang.Integer.reverseBytes(dis.readInt()))
  }

}