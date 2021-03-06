//
// ============================================================================
// (C) Copyright Schalk W. Cronje 2013-2019
//
// This software is licensed under the Apache License 2.0
// See http://www.apache.org/licenses/LICENSE-2.0 for license details
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.
//
// ============================================================================
//

package org.ysb33r.gradle.ivypot.remote

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.IgnoreIf
import spock.lang.Specification


class BinaryDownloaderSpec extends Specification {

    @Rule
    TemporaryFolder testFolder

    void 'Convert dependency to relativePath with classifier'() {
        when:
        def dep = new BinaryDependency(
                organisation: 'foo',
                module: 'bar',
                revision: '1.2.3',
                extension: 'tar',
                classifier: 'bin'
        )
        def pat = '[module]:[revision]:([classifier]:)[ext]'

        then:
        BinaryDownloader.makeRelativePath(pat,dep) == 'bar:1.2.3:bin:tar'
    }

    void 'Convert dependency to relativePath with optional classifier'() {
        when:
        def dep = new BinaryDependency(
                organisation: 'foo',
                module: 'bar',
                revision: '1.2.3',
                extension: 'tar'
        )
        def pat = '[module]:[revision]:([classifier]:)[ext]'

        then:
        BinaryDownloader.makeRelativePath(pat,dep) == 'bar:1.2.3:tar'
    }

    @IgnoreIf({ System.getProperty('OFFLINE') })
    void 'Can download a binary'() {
        setup:
        File repoRoot = testFolder.root
        def repositories = [ nodejs: new BinaryRepositoryDescriptor(
                rootUri: 'https://nodejs.org/dist/'.toURI(),
                artifactPattern: 'v[revision]/[module]-v[revision]-[classifier].[ext]'
        )]
        def downloader = new BinaryDownloader(repoRoot,repositories, true)
        def binaries = [new BinaryDependency(
                organisation: 'nodejs',
                module: 'node',
                revision: '7.10.0',
                classifier: 'linux-x64',
                extension: 'tar.xz'
        )]
        def expectedFile = new File(repoRoot,'nodejs/v7.10.0/node-v7.10.0-linux-x64.tar.xz')

        when:
        downloader.resolve(binaries, true)

        then:
        expectedFile.exists()

        when:
        long lastModified = expectedFile.lastModified()
        downloader.resolve(binaries, false)

        then:
        expectedFile.exists()
        lastModified == expectedFile.lastModified()

        when:
        downloader.resolve(binaries, true)

        then:
        expectedFile.exists()
        // The Travis CI environment does not deal with lastModified correctly
        lastModified != expectedFile.lastModified() || System.getenv('TRAVIS')
    }
}
